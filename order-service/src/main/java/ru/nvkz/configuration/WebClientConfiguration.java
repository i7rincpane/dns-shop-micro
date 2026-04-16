package ru.nvkz.configuration;


import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.shaded.com.google.protobuf.ServiceException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.util.retry.Retry;

import java.time.Duration;


@Slf4j
@Configuration
@EnableConfigurationProperties(RetryDefaultProperty.class)
public class WebClientConfiguration {

    @Value("${services.cart-service.url}")
    private String cartServiceUrl;

    @Value("${services.product-service.url}")
    private String productServiceUrl;

    @Bean
    public WebClient.Builder webClientBuilder() {
        return WebClient.builder();
    }

    @Bean
    public WebClient cartWebClient(WebClient.Builder builder) {
        return builder
                .baseUrl(cartServiceUrl)
                .build();
    }

    @Bean
    public WebClient productWebClient(WebClient.Builder builder) {
        return builder
                .baseUrl(productServiceUrl)
                .build();
    }


    @Bean
    public Retry retryDefault (RetryDefaultProperty property){

        return Retry.backoff(property.maxAttempts(), Duration.ofSeconds(property.minBackoff()))
                .jitter(property.jitter())
                .filter(ex -> ex instanceof ServiceException)
                .onRetryExhaustedThrow((retryBackoffSpec, retrySignal) -> {
                    log.error("All attempts ({}) have been exhausted", retrySignal.totalRetries());
                    return retrySignal.failure();
                });

    }

}
