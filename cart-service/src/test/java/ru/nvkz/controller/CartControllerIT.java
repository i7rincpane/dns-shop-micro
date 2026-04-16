package ru.nvkz.controller;

import liquibase.license.User;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient;
import org.springframework.data.relational.core.query.Criteria;
import org.springframework.data.relational.core.query.Query;
import org.springframework.test.web.reactive.server.WebTestClient;
import ru.nvkz.BaseIntegrationTest;
import ru.nvkz.domain.CartItem;
import ru.nvkz.dto.CartItemDto;
import ru.nvkz.dto.CartItemRequest;
import ru.nvkz.dto.CartItemUpdateDto;
import ru.nvkz.dto.CartResponse;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Stream;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.assertThat;

@AutoConfigureWebTestClient
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class CartControllerIT extends BaseIntegrationTest {

    private static final Long USER_ID = 1L;
    private static final Long PRODUCT_ID_NOT_EXIST = 1L;
    private static final Long PRODUCT_ID_2 = 2L;
    private static final Long PRODUCT_ID_3 = 3L;
    private static final Long PRODUCT_ID_IS_SELECTED_FALSE = 4L;

    @Autowired
    private WebTestClient webTestClient;


    @BeforeEach
    void init() {
        template.insert(new CartItem(null, USER_ID, PRODUCT_ID_2, 1, true, null))
                .then(template.insert(
                        new CartItem(null, USER_ID, PRODUCT_ID_3, 30, true, null)))
                .then(template.insert(
                        new CartItem(null, USER_ID, PRODUCT_ID_IS_SELECTED_FALSE, 3, false, null)))
                .block();


    }

    @Test
    void shouldReturnFullCartWithProductDetails() {

        wireMock.stubFor(get(urlPathEqualTo("/api/v1/products"))
                .withQueryParam("ids", equalTo(PRODUCT_ID_2.toString()))
                .withQueryParam("ids", equalTo(PRODUCT_ID_3.toString()))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                [
                                {"id": 2, "name": "Товар 2", "price": 2200.00, "categoryName": "Электроника", "quantity": 100},
                                {"id": 3, "name": "Товар 3", "price": 2000.00, "categoryName": "Электроника", "quantity": 100}
                                ]
                                """)));

        webTestClient.get().uri("/api/v1/cart")
                .header("X-User-Id", USER_ID.toString())
                .exchange()
                .expectStatus().isOk()
                .expectBody(CartResponse.class)
                .value(cartResponse -> {
                    Assertions.assertNotNull(cartResponse);
                    assertThat(cartResponse.totalSelectedPrice()).isEqualTo(new BigDecimal("62200.00"));
                    assertThat(cartResponse.items())
                            .hasSize(2)
                            .containsExactlyInAnyOrder(
                                    new CartItemDto(PRODUCT_ID_2, "Электроника Товар 2", new BigDecimal("2200.00"), 1, true),
                                    new CartItemDto(PRODUCT_ID_3, "Электроника Товар 3", new BigDecimal("2000.00"), 30, true)
                            );
                });
    }

    @ParameterizedTest(name = "{index} => {2}")
    @MethodSource("getParamsForShouldAddCartItem")
    void shouldAddCartItem(CartItemRequest request, int expectedQuantity, String message) {
        wireMock.stubFor(get(urlPathEqualTo("/api/v1/products"))
                .withQueryParam("ids", equalTo(PRODUCT_ID_2.toString()))
                .willReturn(aResponse()
                        .withHeader("Content-type", "application/json")
                        .withBody("""
                                [
                                {"id": 2, "name": "Товар 2", "price": 2200.00, "categoryName": "Электроника", "quantity": 100}
                                ]
                                """)));

        wireMock.stubFor(get(urlPathEqualTo("/api/v1/products"))
                .withQueryParam("ids", equalTo(PRODUCT_ID_NOT_EXIST.toString()))
                .willReturn(aResponse()
                        .withHeader("Content-type", "application/json")
                        .withBody("""
                                [
                                {"id": 1, "name": "Товар 1", "price": 800.00, "categoryName": "Электроника", "quantity": 100}
                                ]
                                """)
                )
        );


        webTestClient.post().uri("/api/v1/cart")
                .header("X-User-Id", USER_ID.toString())
                .bodyValue(request)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.productId")
                .isEqualTo(request.productId())
                .jsonPath("$.quantity")
                .isEqualTo(expectedQuantity);
    }

    @Test
    void shouldUpdateCartItem() {
        wireMock.stubFor(get(urlPathEqualTo("/api/v1/products"))
                .withQueryParam("ids", equalTo(PRODUCT_ID_3.toString()))
                .willReturn(aResponse()
                        .withHeader("Content-type", "application/json")
                        .withBody("""
                                [
                                {"id": 3, "name": "Товар 3", "price": 2000.00, "categoryName": "Электроника", "quantity": 100}
                                ]
                                """)));

        webTestClient.patch().uri("/api/v1/cart/{id}", PRODUCT_ID_3)
                .header("X-User-Id", USER_ID.toString())
                .bodyValue(new CartItemUpdateDto(5, false))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.quantity").isEqualTo(5)
                .jsonPath("$.selected").isEqualTo(false);

    }

    @Test
    void shouldRemoveItemWhenQuantityIsZero() {
        webTestClient.patch().uri("/api/v1/cart/{id}", PRODUCT_ID_3)
                .header("X-User-Id", USER_ID.toString())
                .bodyValue(new CartItemUpdateDto(0, null))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .isEmpty();

        assertThat(template.count(Query.query(Criteria.where("user_id").is(USER_ID)
                        .and("product_id").is(PRODUCT_ID_3)
                ), CartItem.class)
                .block()).isZero();

    }

    @Test
    void shouldClearProductIds() {
        webTestClient.delete().uri(uriBuilder -> uriBuilder.path("/api/v1/cart")
                        .queryParam("ids", List.of(PRODUCT_ID_2, PRODUCT_ID_3))
                        .build())
                .header("X-User-Id", USER_ID.toString())

                .exchange()
                .expectStatus().isNoContent();

        List<CartItem> actualProducts = template.select(Query.query(Criteria.where("user_id").is(USER_ID)), CartItem.class)
                .collectList()
                .block();

        assertThat(actualProducts)
                .isNotEmpty()
                .hasSize(1)
                .first()
                .extracting(CartItem::getProductId)
                .isEqualTo(PRODUCT_ID_IS_SELECTED_FALSE);
    }

    private static Stream<Arguments> getParamsForShouldAddCartItem() {
        return Stream.of(
                Arguments.of(new CartItemRequest(PRODUCT_ID_2, 10), 11, "Increment existing"),
                Arguments.of(new CartItemRequest(PRODUCT_ID_NOT_EXIST, 10), 10, "Create new"),
                Arguments.of(new CartItemRequest(PRODUCT_ID_2, 101), 100, "Increment with min product quantity"),
                Arguments.of(new CartItemRequest(PRODUCT_ID_NOT_EXIST, 101), 100, "Create new with min product quantity"));
    }
}