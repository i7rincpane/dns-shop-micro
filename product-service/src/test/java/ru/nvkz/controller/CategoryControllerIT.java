package ru.nvkz.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient;
import org.springframework.test.web.reactive.server.WebTestClient;
import ru.nvkz.BaseIntegrationTest;
import ru.nvkz.domain.Category;

import static org.assertj.core.api.Assertions.assertThat;

@AutoConfigureWebTestClient
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class CategoryControllerIT extends BaseIntegrationTest {

    @Autowired
    private WebTestClient webTestClient;

    @Test
    void shouldCreateAndGetAll() {
        webTestClient.post().uri("/api/v1/categories")
                .bodyValue(new Category(null, "Электроника"))
                .exchange()
                .expectStatus().isCreated()
                .expectBody(Category.class)
                .returnResult()
                .getResponseBody();

        webTestClient.get().uri("/api/v1/categories")
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(Category.class)
                .hasSize(1)
                .value(list -> assertThat(list.get(0).name()).isEqualTo("Электроника"));
    }
}