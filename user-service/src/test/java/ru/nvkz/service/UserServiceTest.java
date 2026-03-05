package ru.nvkz.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import reactor.test.StepVerifier;
import ru.nvkz.BaseIntegrationTest;
import ru.nvkz.dto.RegistrationRequest;
import ru.nvkz.repository.UserRepository;

@SpringBootTest
class UserServiceTest extends BaseIntegrationTest {

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @Test
    void shouldRollbackTransactionOnFailure() {

        var badRequest = new RegistrationRequest("test5@test.com", "pass", "name".repeat(500), "Surname");

        StepVerifier.create(userService.create(badRequest))
                .expectError()
                .verify();
        StepVerifier.create(userRepository.findByEmail("fail@test.com"))
                .expectNextCount(0)
                .verifyComplete();
    }
}