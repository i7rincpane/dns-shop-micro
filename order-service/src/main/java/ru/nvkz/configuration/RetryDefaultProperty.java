package ru.nvkz.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.retry")
public record RetryDefaultProperty(
        int maxAttempts,
        int minBackoff,
        double jitter
) {
}