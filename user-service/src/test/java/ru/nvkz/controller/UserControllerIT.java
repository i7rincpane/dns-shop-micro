package ru.nvkz.controller;


import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient;
import org.springframework.test.web.reactive.server.WebTestClient;
import ru.nvkz.BaseIntegrationTest;
import ru.nvkz.domain.User;
import ru.nvkz.dto.RegistrationRequest;
import ru.nvkz.dto.UserUpdateDto;

import java.time.LocalDate;
import java.time.Month;
import java.util.stream.Stream;

@AutoConfigureWebTestClient
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class UserControllerIT extends BaseIntegrationTest {

    @Autowired
    private WebTestClient webTestClient;

    @Test
    void shouldCreateUserAndFindByIdAndFindAllByFilter() {
        RegistrationRequest registrationRequest = new RegistrationRequest(
                "test@mail.ru", "password", "Ivan", "Ivanov", "", LocalDate.of(1992, Month.FEBRUARY, 23)
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
                .expectBody()
                .jsonPath("$.email").isEqualTo("test@mail.ru")
                .jsonPath("$.name").isEqualTo("Ivan");


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
    void shouldReturn404WhenUserNotFound() {
        webTestClient.get().uri("/api/v1/users/{id}", 999)
                .header("Accept-Language", "en")
                .exchange()
                .expectStatus().isNotFound()
                .expectBody(String.class)
                .isEqualTo("User with id 999 not found");
    }

    @ParameterizedTest
    @MethodSource("invalidRegistrationRequests")
    void shouldReturn400WhenRequestIsInvalid(RegistrationRequest badRequest, String expectedField) {
        webTestClient.post().uri("/api/v1/users")
                .bodyValue(badRequest)
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$." + expectedField).exists();
    }

    @Test
    void shouldPatchUserProfile() {
        var regRequest = new RegistrationRequest("patch@test.com", "pass12345", "Ivan", "Ivanov", "123", LocalDate.now().minusYears(20));
        User savedUser = webTestClient.post().uri("/api/v1/users").bodyValue(regRequest).exchange().returnResult(User.class).getResponseBody().blockFirst();

        var updateDto = new UserUpdateDto(null, null, "777-777", null);

        webTestClient.patch().uri("/api/v1/users/{id}", savedUser.id())
                .bodyValue(updateDto)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.phone").isEqualTo("777-777")
                .jsonPath("$.name").isEqualTo("Ivan");

    }


    private static Stream<Arguments> invalidRegistrationRequests() {
        return Stream.of(
                Arguments.of(new RegistrationRequest("not-an-email", "88888888", "Ivan", "Ivanov", "", LocalDate.of(1992, Month.FEBRUARY, 23)), "email"),
                Arguments.of(new RegistrationRequest("", "88888888", "Ivan", "Ivanov", "", LocalDate.of(1992, Month.FEBRUARY, 23)), "email"),
                Arguments.of(new RegistrationRequest("test@mail.ru", "123", "Ivan", "Ivanov", "", LocalDate.of(1992, Month.FEBRUARY, 23)), "password"),
                Arguments.of(new RegistrationRequest("test@mail.ru", "88888888", "", "Ivanov", "", LocalDate.of(1992, Month.FEBRUARY, 23)), "name"),
                Arguments.of(new RegistrationRequest("test@mail.ru", "88888888", "Ivan", "Ivanov", "", LocalDate.now().plusDays(1)), "birthdate")
        );
    }
}