package ru.nvkz.listener;

import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class CdcTestListener {

    // Слушаем топик, который сгенерировал Debezium
    @KafkaListener(topics = "cdc.public.outbox_events", groupId = "cdc-test-group")
    public void listenCdc(String rawJson) {
        log.info("[CDC УСПЕХ] Debezium перехватил запись напрямую из WAL-лога Postgres!");
        log.info("Данные от Debezium: {}", rawJson);
    }
}


