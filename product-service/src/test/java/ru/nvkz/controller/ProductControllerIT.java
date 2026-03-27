package ru.nvkz.controller;


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
import reactor.core.publisher.Flux;
import ru.nvkz.BaseIntegrationTest;
import ru.nvkz.domain.Category;
import ru.nvkz.domain.Product;
import ru.nvkz.dto.*;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

@AutoConfigureWebTestClient
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ProductControllerIT extends BaseIntegrationTest {

    public static final long CATEGORY_ID = 1L;
    @Autowired
    private WebTestClient webTestClient;

    @BeforeEach
    void init() {
        template.insert(new Category(null, "Электроника"))
                .flatMap(category ->
                        Flux.concat(
                                template.insert(createNewProduct(category.id(), "Товар 1", new BigDecimal("100.00"), "Дешевый", Map.of("color", "black", "storage", "256GB"))),
                                template.insert(createNewProduct(category.id(), "Товар 2", new BigDecimal("500.00"), "Приемлемый", Map.of("color", "white", "storage", "128GB"))),
                                template.insert(createNewProduct(category.id(), "Товар 3", new BigDecimal("1000.00"), "Дорогой", Map.of("color", "black", "fast_charge", true))),
                                template.insert(createNewProduct(category.id(), "Другое", new BigDecimal("1101.00"), "Переменный", Collections.emptyMap()))
                        ).collectList()
                ).then(template.insert(new Category(null, "Посуда")))
                .flatMap(category ->
                        template.insert(createNewProduct(category.id(), "Тарелка 1", new BigDecimal("100.00"), "Дешевый", Collections.emptyMap()))
                )
                .block();
    }

    @Test
    void shouldReturnProductsByIds() {
        webTestClient.get().uri(uriBuilder -> uriBuilder
                        .path("/api/v1/products")
                        .queryParam("ids", 3L, 4L)
                        .build())
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(ProductFullResponse.class)
                .value(response -> {
                    assertThat(response).extracting(ProductFullResponse::name)
                            .containsExactlyInAnyOrder("Товар 3", "Другое");
                });
    }

    @Test
    void shouldReturnFiltersByCategoryId() {
        webTestClient.get().uri(uriBuilder -> uriBuilder
                        .path("/api/v1/products/filters")
                        .queryParam("categoryId", CATEGORY_ID)
                        .build())
                .exchange()
                .expectStatus().isOk()
                .expectBody(CategoryFiltersResponse.class)
                .value(response -> {
                    Collection<FilterValue> filterValues = response.attributes().get("color");
                    assertThat(filterValues).containsExactlyInAnyOrder(new FilterValue("black", 2L), new FilterValue("white", 1L));
                });
    }

    @Test
    void shouldCreateAndFindByIdAndFindAllByFilter() {
        Category category = template
                .selectOne(Query.query(Criteria.where("id").is(CATEGORY_ID)), Category.class)
                .block();

        Product product = webTestClient.post().uri("/api/v1/products")
                .bodyValue(new ProductSaveDto(category.id(), "Ноутбук", new BigDecimal("500000.00"), "Мощный", Map.of("CPU", "M3", "RAM", "16GB")))
                .exchange()
                .expectStatus().isCreated()
                .expectBody(Product.class)
                .returnResult().getResponseBody();

        webTestClient.get().uri("/api/v1/products/{id}", product.getId())
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.name").isEqualTo("Ноутбук")
                .jsonPath("$.categoryName").isEqualTo("Электроника")
                .jsonPath("$.attributes.CPU").isEqualTo("M3");
    }

    @Test
    void shouldReturnSecondPageOfProducts() {
        webTestClient.get().uri(uriBuilder -> uriBuilder
                        .path("/api/v1/products")
                        .queryParam("pageSize", 1)
                        .queryParam("pageNumber", 1)
                        .build())
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(ProductFullResponse.class)
                .hasSize(1)
                .value(list -> assertThat(list.get(0).name()).isEqualTo("Товар 2"));

    }

    @ParameterizedTest
    @MethodSource("filterProductsByPriceRange")
    void shouldFilterProductsByFilter(ProductSearchRequest filter, int expectedSize, String[] expectedNames) {
        webTestClient.get().uri(uriBuilder -> {
                            uriBuilder.path("/api/v1/products");
                            if (filter.maxPrice() != null) uriBuilder.queryParam("maxPrice", filter.maxPrice());
                            if (filter.minPrice() != null) uriBuilder.queryParam("minPrice", filter.minPrice());
                            if (filter.namePart() != null) uriBuilder.queryParam("namePart", filter.namePart());
                            if (filter.categoryId() != null) uriBuilder.queryParam("categoryId", filter.categoryId());
                            if (filter.attrs() != null) uriBuilder.queryParam("attrs", filter.attrs().toArray());
                            return uriBuilder.build();
                        }
                )
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(ProductFullResponse.class)
                .hasSize(expectedSize)
                .value(list -> assertThat(list)
                        .extracting(ProductFullResponse::name)
                        .containsExactlyInAnyOrder(expectedNames));
    }

    @Test
    void shouldReturn404WhenProductNotFound() {
        webTestClient.get().uri("/api/v1/products/{id}", 999)
                .header("Accept-Language", "en")
                .exchange()
                .expectStatus().isNotFound()
                .expectBody(String.class)
                .isEqualTo("Product with id 999 not found");
    }

    @Test
    void shouldReturn$404WhenCategoryIntoProductNotExisting() {
        webTestClient.post().uri("/api/v1/products")
                .header("Accept-Language", "ru")
                .bodyValue(new ProductSaveDto(999L, "Ноутбук", new BigDecimal("500000.00"), "Мощный", Collections.emptyMap()))
                .exchange()
                .expectStatus().isNotFound()
                .expectBody(String.class)
                .isEqualTo("Категория 999 не найдена");
    }

    @Test
    void shouldMergeAttributesOnPatch() {
        var updateDto = new ProductUpdateDto(null, null, null, null, Map.of("color", "white", "CPU", "M3"));

        webTestClient.patch().uri("/api/v1/products/{id}", 1L)
                .bodyValue(updateDto)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.attributes.color").isEqualTo("white")
                .jsonPath("$.attributes.storage").isEqualTo("256GB")
                .jsonPath("$.attributes.CPU").isEqualTo("M3");
    }

    @ParameterizedTest
    @MethodSource("invalidCreateProduct")
    void shouldReturn400WhenCreateIsInvalid(ProductSaveDto dto, String expectedField) {
        webTestClient.post().uri("/api/v1/products")
                .bodyValue(dto)
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$." + expectedField).exists();

    }

    private static Stream<Arguments> invalidCreateProduct() {
        return Stream.of(
                Arguments.of(new ProductSaveDto(null, "Ноутбук", new BigDecimal("500000.00"), "Мощный", Collections.emptyMap()), "categoryId"),
                Arguments.of(new ProductSaveDto(CATEGORY_ID, "", new BigDecimal("500000.00"), "Мощный", Collections.emptyMap()), "name"),
                Arguments.of(new ProductSaveDto(CATEGORY_ID, null, new BigDecimal("500000.00"), "Мощный", Collections.emptyMap()), "name"),
                Arguments.of(new ProductSaveDto(CATEGORY_ID, "   ", new BigDecimal("500000.00"), "Мощный", Collections.emptyMap()), "name"),
                Arguments.of(new ProductSaveDto(CATEGORY_ID, "Ноутбук", null, "Мощный", Collections.emptyMap()), "price"),
                Arguments.of(new ProductSaveDto(CATEGORY_ID, "Ноутбук", new BigDecimal("-1.00"), "Мощный", Collections.emptyMap()), "price")
        );
    }

    private static Stream<Arguments> filterProductsByPriceRange() {
        return Stream
                .of(
                        Arguments.of(new ProductSearchRequest(null, null, null, null, Collections.emptyList()), 5, new String[]{"Товар 1", "Товар 2", "Товар 3", "Другое", "Тарелка 1"}),
                        Arguments.of(new ProductSearchRequest("2", null, null, null, Collections.emptyList()), 1, new String[]{"Товар 2"}),
                        Arguments.of(new ProductSearchRequest("Товар", null, null, null, Collections.emptyList()), 3, new String[]{"Товар 1", "Товар 2", "Товар 3"}),
                        Arguments.of(new ProductSearchRequest(null, null, null, new BigDecimal(400L), Collections.emptyList()), 2, new String[]{"Товар 1", "Тарелка 1"}),
                        Arguments.of(new ProductSearchRequest(null, null, new BigDecimal(600L), null, Collections.emptyList()), 2, new String[]{"Товар 3", "Другое"}),
                        Arguments.of(new ProductSearchRequest(null, null, new BigDecimal(400L), new BigDecimal(1001L), Collections.emptyList()), 2, new String[]{"Товар 2", "Товар 3"}),
                        Arguments.of(new ProductSearchRequest(null, 2L, null, null, Collections.emptyList()), 1, new String[]{"Тарелка 1"}),
                        Arguments.of(new ProductSearchRequest(null, null, null, null, List.of("color:black")), 2, new String[]{"Товар 1", "Товар 3"})
                );
    }

    private Product createNewProduct(Long categoryId, String name, BigDecimal price, String description, Map<String, Object> attrs) {
        return new Product(null, categoryId, name, price, description, attrs, null);
    }

}