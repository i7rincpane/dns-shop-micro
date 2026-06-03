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
     * Используется на стороне ОТПРАВИТЕЛЯ (Producer / Scheduler).
     * Метод берет старые ID из БД, делает текущий сервис "родителем", создает
     * новую операцию (Next Span) и вшивает её заголовки в исходящее сообщение Kafka.
     * 1. Восстанавливаем родительский контекст (parentContext) из сохраненных в БД traceId и spanId.
     * 2. Создаем заготовку спана билдером (через tracer.spanBuilder()), указав восстановленный контекст как "родителя".
     * 3. Создаем обсервер (observation).
     * 4. Запускаем спан (parentSpan) и временно кладем его в tracer (в ThreadLocal поток Java), чтобы сделать активным.
     * 5. Обсервер при старте (observation.start()) заглядывает в tracer, видет запущенный спан, связывает себя с его
     * Trace ID и запускает метрики (таймер).
     * 6. Извлекаем текущий склеенный ID из трейсера и принудительно вшиваем его в заголовки исходящего сообщения Kafka
     * (propagator.inject).
     * 7. Передаем обсервер в реактивный контекст (.contextWrite) внутренней бизнес-цепочки, чтобы логи (MDC) работали.
     * 8. Когда вся цепочка отправки отработала: таймер остановили (observation.stop()), а спан, созданный нами вручную,
     * удалили из памяти (.end()).
     */
    @SuppressWarnings("PMD.UnusedLocalVariable")
    public <T> Mono<T> executeWithNextKafkaSpan(
            String traceId,
            String spanId,
            String operationName,
            ProducerRecord<String, String> kafkaRecord,
            Supplier<Mono<T>> reactiveAction
    ) {
        TraceContext parentContext = tracer.traceContextBuilder()
                .traceId(traceId)
                .spanId(spanId)
                .sampled(true)
                .build();

        Observation observation = Observation.createNotStarted(operationName, observationRegistry);

        observation.parentObservation(null);

        return Mono.defer(() -> {

            Span parentSpan = tracer.spanBuilder().setParent(parentContext).start();

            try (Tracer.SpanInScope parentScope = tracer.withSpan(parentSpan)) {

                observation.start();

                propagator.inject(tracer.currentSpan().context(), kafkaRecord,
                        (rec, key, val) -> rec.headers().add(key, val.getBytes()));

                return reactiveAction.get()
                        .contextWrite(ctx -> ctx.put(ObservationThreadLocalAccessor.KEY, observation))
                        .doFinally(signal -> {
                            observation.stop();
                            parentSpan.end();
                        });
            }
        });
    }

    /**
     * Используется на стороне ПОЛУЧАТЕЛЯ (Consumer / @KafkaListener).
     * Метод берет входящие бинарные заголовки сообщения,
     * достает из них сохраненный traceparent, восстанавливает дерево трассировки
     * и делает текущий метод ребенком (продолжением) асинхронной цепочки.
     * <p>
     * 1. Создали заготовку спана билдером (spanBuilder).
     * 2. Запустили этот спан (tempSpan) и положили его в tracer (в поток), чтобы сделать активным.
     * 3. Создали обсервер (observation).
     * 4. Обсервер при старте заглянул в tracer, увидел запущенный спан, забрал его traceId и запустил метрики (таймер).
     * 5. Передали обсервер в реактивный контекст(contextWrite) бизнесс
     * (у каждой цепочки свой реактивный контекст).
     * 6. Когда вся цепочка отработала: таймер остановили(stop),
     * а спан, созданный нами вручную, удалили из памяти (.end()).
     */
    @SuppressWarnings("PMD.UnusedLocalVariable")
    public <T> Mono<T> executeAsKafkaConsumer(
            org.apache.kafka.common.header.Headers kafkaHeaders,
            String operationName,
            String contextualName,
            String topic,
            Supplier<Mono<T>> reactiveAction
    ) {

        Map<String, String> headersMap = new HashMap<>();
        kafkaHeaders.forEach(h -> headersMap.put(h.key(), new String(h.value())));


        Span.Builder spanBuilder = propagator.extract(headersMap, Map::get);


        Observation observation = Observation.createNotStarted(operationName, observationRegistry)
                .contextualName(contextualName)
                .lowCardinalityKeyValue("kafka.topic1", topic);


        observation.parentObservation(null);
        return Mono.defer(() -> {
            var tempSpan = spanBuilder.start();
            try (Tracer.SpanInScope ps = tracer.withSpan(tempSpan)) {
                observation.start();
                return reactiveAction.get()
                        .contextWrite(ctx -> ctx.put(ObservationThreadLocalAccessor.KEY, observation))
                        .doFinally(signal -> {
                            observation.stop();
                            tempSpan.end();
                        });
            }
        });
    }
}
