package ru.nvkz.configuration;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.kafka.autoconfigure.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.kafka.sender.KafkaSender;
import reactor.kafka.sender.SenderOptions;

import java.util.Map;

@Slf4j
@Configuration
public class KafkaConfig {

    @Bean
    public KafkaSender<String, String> kafkaSender(KafkaProperties kafkaProperties) {
        Map<String, Object> props = kafkaProperties.buildProducerProperties();
        log.info("Settings kafkaSender:{}", props);
        return KafkaSender.create(SenderOptions.create(props));
    }
}