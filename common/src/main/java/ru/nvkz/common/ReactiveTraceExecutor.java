package ru.nvkz.common;

import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import io.micrometer.observation.contextpropagation.ObservationThreadLocalAccessor;
import io.micrometer.tracing.Span;
import io.micrometer.tracing.TraceContext;
import io.micrometer.tracing.Tracer;
import io.micrometer.tracing.propagation.Propagator;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.producer.ProducerRecord;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;


@RequiredArgsConstructor
public class ReactiveTraceExecutor {

    private final Tracer tracer;
    private final Propagator propagator;
    private final ObservationRegistry observationRegistry;


    /**
     * НАЗВАНИЕ: Выполнить со следующим Kafka-спаном
     * СМЫСЛ: Используется на стороне ОТПРАВИТЕЛЯ (Producer / Scheduler).
     * Метод берет старые ID из БД, делает текущий сервис "родителем", создает
     * новую операцию (Next Span) и вшивает её заголовки в исходящее сообщение Kafka.
     */
    public <T> Mono<T> executeWithNextKafkaSpan(
            String traceId,
            String spanId,
            String operationName, // Имя операции для Zipkin
            ProducerRecord<String, String> kafkaRecord,
            Supplier<Mono<T>> reactiveAction
    ) {
        // Восстанавливаем родительский контекст из БД
        TraceContext parentContext = tracer.traceContextBuilder()
                .traceId(traceId)
                .spanId(spanId)
                .sampled(true)
                .build();
        /*
        Создаем Observation. В реактивном контексте автоматическое копирование (MDC логов)
        завязано на него, сам создает метрики(замеры) и спаны(история операций).
        observationRegistry - регистрирует все обработчики.
        */
        Observation observation = Observation.createNotStarted(operationName, observationRegistry);
        observation.parentObservation(null);

        return Mono.defer(() -> {
                    Span parentSpan = tracer.spanBuilder().setParent(parentContext).start();
            
                     /*
                     Кладем спан в область видимости потока. withSpan делает его видимым(активным в текущем потоке)
                     для блока try (кладет спан в ThreadLocal).
                     автоматически вызывает close() (делает неактивным) для устранения утечки памяти.
                     start - запускаем микро-спан, чтобы наблюдатель его увидел.
                   */
                    try (Tracer.SpanInScope parentScope = tracer.withSpan(parentSpan)) {

                        observation.start(); // смотрит что включен зипкин и создает спан, запускат таймер операции

                        /*
                        Область видимости TraceId для логов (MDC).
                        Логер читает TraceID из MDC Обертки TreadLocal, привязанный к каждому потоку.
                        openScope, забирает спан, извлекает из него traceId и помещает в MDC текущего потока.
                        В блоке try, любой лог увидет этот traceId.
                        После выхода из блока scope.close() - TraceID стирается,
                        чтобы следующие операции его не увидили
                        */
                        try (Observation.Scope scope = observation.openScope()) {

                            // Приводим ID(val) к шаблону, и добавляем в заголовки(key - по умолчанию traceparent) Kafka
                            propagator.inject(tracer.currentSpan().context(), kafkaRecord,
                                    (rec, key, val) -> rec.headers().add(key, val.getBytes()));

                            // Выполняем бизнес-логику (отправку в кафку)
                            return reactiveAction.get()
                                    .doFinally(signal -> {
                                        // закрываем оба объекта по завершении потока
                                        observation.stop(); // Закрывает спан публикации и шлет в Zipkin
                                        parentSpan.end();   // Очищает память Brave от родительского спана-декоратора
                                    });
                        }
                    }
                })
                // Прокидываем в контекст Реактора для сохранения ID между операторами
                .contextWrite(ctx -> ctx.put(ObservationThreadLocalAccessor.KEY, observation));
    }

    /**
     * НАЗВАНИЕ: Выполнить как потребитель Kafka
     * СМЫСЛ: Используется на стороне ПОЛУЧАТЕЛЯ (Consumer / @KafkaListener).
     * Метод не знает никаких ID заранее. Он берет входящие бинарные заголовки сообщения,
     * достает из них сохраненный traceparent, восстанавливает дерево трассировки
     * и делает текущий метод ребенком (продолжением) асинхронной цепочки.
     */
    public <T> Mono<T> executeAsKafkaConsumer(
            org.apache.kafka.common.header.Headers kafkaHeaders,
            String operationName, // operationName для группировки метрик
            String contextualName,  // contextualName для Span Name в Zipkin
            String topic,
            Supplier<Mono<T>> reactiveAction
    ) {
        // 1. Извлекаем заголовки в Map
        Map<String, String> headersMap = new HashMap<>();
        kafkaHeaders.forEach(h -> headersMap.put(h.key(), new String(h.value())));

        // 2. Восстанавливаем родительский контекст (вернут Span.Builder)
        var spanBuilder = propagator.extract(headersMap, Map::get);

        // 3. Создаем Observation
        Observation observation = Observation.createNotStarted(operationName, observationRegistry)
                .contextualName(contextualName)
                .lowCardinalityKeyValue("kafka.topic", topic);

        return Mono.defer(() -> {
                    // Старуем временный спан на основе извлеченных данных, чтобы Observation "приклеилась" к нему
                    var tempSpan = spanBuilder.start();

                    try (Tracer.SpanInScope ps = tracer.withSpan(spanBuilder.start())) {

                        observation.start();

                        try (Observation.Scope scope = observation.openScope()) {
                            return reactiveAction.get()
                                    .doFinally(signal -> {
                                        observation.stop();
                                        tempSpan.end(); // Обязательно закрываем временный спан, чтобы не было утечек!
                                    });
                        }
                    }
                })
                .contextWrite(ctx -> ctx.put(ObservationThreadLocalAccessor.KEY, observation));
    }

}
