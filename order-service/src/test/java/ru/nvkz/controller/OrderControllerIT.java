package ru.nvkz.controller;

import com.github.tomakehurst.wiremock.client.WireMock;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient;
import org.springframework.test.web.reactive.server.WebTestClient;
import ru.nvkz.BaseIntegrationTest;
import ru.nvkz.domain.Order;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@AutoConfigureWebTestClient
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class OrderControllerIT extends BaseIntegrationTest {

    @Autowired
    private WebTestClient webTestClient;

    @Test
    void shouldCreateOrderSuccessfully() {
        Long userId = 1L;

        wireMock.stubFor(WireMock.get(WireMock.urlEqualTo("/api/v1/cart"))
                .withHeader("X-User-Id", WireMock.equalTo(userId.toString()))
                .willReturn(WireMock.aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                    {
                                        "items": [{
                                            "productId": 10,
                                            "productName": "Laptop",
                                            "price": 1500.0,
                                            "quantity": 1,
                                            "isSelected": true
                                        }],
                                        "totalSelectedPrice": 1500.0
                                    }
                                """)));

        wireMock.stubFor(WireMock.post(WireMock.urlEqualTo("/api/v1/products/stock/decrease"))
                .willReturn(WireMock.ok()));

        webTestClient.post().uri("/api/v1/orders")
                .header("X-User-Id", userId.toString())
                .exchange()
                .expectStatus().isCreated()
                .expectBody()
                .jsonPath("$.status").isEqualTo("NEW")
                .jsonPath("$.totalPrice").isEqualTo(1500.0);

        Long count = template.select(Order.class).count().block();
        assertThat(count).isEqualTo(1);

    }

    @Test
    void shouldRestoreStockIfOrderSavingFails() {
        Long userId = 2L;

        wireMock.stubFor(WireMock.get(WireMock.urlEqualTo("/api/v1/cart"))
                .willReturn(WireMock.okJson("""
                {"items": [{"productId": 99, "productName": null, "quantity": 1, "isSelected": true}], "totalSelectedPrice": 500}
            """)));

        wireMock.stubFor(WireMock.post(WireMock.urlEqualTo("/api/v1/products/stock/decrease"))
                .willReturn(WireMock.ok()));


        wireMock.stubFor(WireMock.post(WireMock.urlEqualTo("/api/v1/products/stock/increase"))
                .willReturn(WireMock.ok()));

        webTestClient.post().uri("/api/v1/orders")
                .header("X-User-Id", userId.toString())
                .exchange()
                .expectStatus().is5xxServerError();

        wireMock.verify(WireMock.postRequestedFor(WireMock.urlEqualTo("/api/v1/products/stock/increase")));
    }

}