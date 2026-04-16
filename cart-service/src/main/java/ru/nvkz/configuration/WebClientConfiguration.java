package ru.nvkz.configuration;


import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;


@Configuration
public class WebClientConfiguration {

    @Value("${services.product-service.url}")
    private String productServiceUrl;

    @Bean
    public WebClient.Builder webClientBuilder() {
        return WebClient.builder();
    }

    @Bean
    public WebClient productWebClient(WebClient.Builder builder) {
        return builder
                .baseUrl(productServiceUrl)
                .build();
    }

}
