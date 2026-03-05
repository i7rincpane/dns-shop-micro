package ru.nvkz.controller;


import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient;
import org.springframework.test.web.reactive.server.WebTestClient;
import ru.nvkz.BaseIntegrationTest;
import ru.nvkz.domain.User;
import ru.nvkz.dto.RegistrationRequest;
import ru.nvkz.dto.UserSearchRequest;

@AutoConfigureWebTestClient
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class UserControllerTest extends BaseIntegrationTest {

    @Autowired
    private WebTestClient webTestClient;

    @Test
    void shouldCreateUserAndFindByIdAndFindAllByFilter() {
        RegistrationRequest registrationRequest = new RegistrationRequest(
                "test@mail.ru", "password123", "Ivan", "Ivanov"
        );

        User savedUser = webTestClient.post().uri("/api/v1/users")
                .bodyValue(registrationRequest)
                .exchange()
                .expectStatus().isCreated()
                .expectBody(User.class)
                .returnResult()
                .getResponseBody();

        webTestClient.get().uri("/api/v1/users/{id}", savedUser.id())
                .exchange()
                .expectStatus().isOk()
                .expectBody()// почему здесь не указываем User.class?
                .jsonPath("$.email").isEqualTo("test@mail.ru")
                .jsonPath("$.name").isEqualTo("Ivan");

        UserSearchRequest searchRequest = new UserSearchRequest(
                "test@mail.ru", null, null
        );

        webTestClient.get().uri(uriBuilder -> uriBuilder
                        .path("/api/v1/users")
                        .queryParam("email", "test@mail.ru")
                        .build())
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(User.class)
                .hasSize(1)
                .contains(savedUser);

    }

    @Test
    void shouldReturn400WhenEmailIsInvalid() {
        RegistrationRequest badRequest = new RegistrationRequest(
                "not-an-email", "", "Ivan", "Ivanov"
        );

        webTestClient.post().uri("/api/v1/users")
                .bodyValue(badRequest)
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.email").exists()
                .jsonPath("$.password").exists();
    }

}