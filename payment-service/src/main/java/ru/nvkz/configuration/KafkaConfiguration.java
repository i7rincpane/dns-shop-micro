package ru.nvkz.configuration;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.kafka.autoconfigure.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.kafka.receiver.KafkaReceiver;
import reactor.kafka.receiver.ReceiverOptions;
import reactor.kafka.sender.KafkaSender;
import reactor.kafka.sender.SenderOptions;

import java.util.Collections;
import java.util.Map;

@Slf4j
@Configuration
public class KafkaConfiguration {

    @Bean
    public KafkaSender<String, String> kafkaSender(KafkaProperties kafkaProperties) {
        Map<String, Object> props = kafkaProperties.buildProducerProperties();
        log.info("Settings kafkaSender:{}", props);
        return KafkaSender.create(SenderOptions.create(props));
    }

    @Bean
    public KafkaReceiver<String, String> kafkaReceiver(KafkaProperties kafkaProperties,
                                                       @Value("${app.order-events-topic.name}") String topic) {
        Map<String, Object> props = kafkaProperties.buildConsumerProperties();
        log.info("Settings kafkaReceiver:{}", props);
        ReceiverOptions<String, String> receiverOptions = ReceiverOptions.<String, String>create(props).subscription(Collections.singleton(topic));
        return KafkaReceiver.create(receiverOptions);
    }
}