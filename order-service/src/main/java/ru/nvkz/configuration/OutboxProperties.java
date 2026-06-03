package ru.nvkz.configuration;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "app.outbox")
public class OutboxProperties {

    private String topic;
    private int limitRate;

}
