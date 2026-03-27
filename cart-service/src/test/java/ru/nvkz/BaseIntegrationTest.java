package ru.nvkz;


import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;

import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;


public abstract class BaseIntegrationTest {


    static PostgreSQLContainer<?> postgreSQLContainer = new PostgreSQLContainer<>("postgres:16-alpine");


    static {
        postgreSQLContainer.start();
    }

    @RegisterExtension
    protected static WireMockExtension wireMock = WireMockExtension.newInstance()
            .options(wireMockConfig().dynamicPort())
            .build();

    @Autowired
    protected R2dbcEntityTemplate template;

    @BeforeEach
    void clearDatabase() {
        template.getDatabaseClient()
                .sql("TRUNCATE TABLE cart_items RESTART IDENTITY CASCADE")
                .then()
                .block();
    }

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.liquibase.url", postgreSQLContainer::getJdbcUrl);
        registry.add("spring.liquibase.user", postgreSQLContainer::getUsername);
        registry.add("spring.liquibase.password", postgreSQLContainer::getPassword);

        registry.add("spring.r2dbc.url", () -> String.format("r2dbc:postgresql://%s:%d/%s",
                postgreSQLContainer.getHost(),
                postgreSQLContainer.getMappedPort(PostgreSQLContainer.POSTGRESQL_PORT),
                postgreSQLContainer.getDatabaseName()));
        registry.add("spring.r2dbc.username", postgreSQLContainer::getUsername);
        registry.add("spring.r2dbc.password", postgreSQLContainer::getPassword);
        System.out.println("порт: " + postgreSQLContainer.getMappedPort(PostgreSQLContainer.POSTGRESQL_PORT));

        registry.add("services.product-service.url", wireMock::baseUrl);
    }

}




