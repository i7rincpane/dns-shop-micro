package ru.nvkz.configuration;


import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;


@Configuration
public class WebClientConfig {

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

}
