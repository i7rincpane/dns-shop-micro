package ru.nvkz.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import ru.nvkz.domain.User;
import ru.nvkz.domain.UserProfile;
import ru.nvkz.dto.RegistrationRequest;
import ru.nvkz.dto.UserFullInfo;
import ru.nvkz.dto.UserSearchFilter;
import ru.nvkz.dto.UserUpdateDto;
import ru.nvkz.service.UserService;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/users")
public class UserController {

    private final UserService userService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<User> create(@Valid @RequestBody RegistrationRequest request) {
        return userService.create(request);
    }

    @GetMapping("/{id}")
    public Mono<UserFullInfo> getById(@PathVariable Long id) {
        return userService.findById(id);
    }

    @GetMapping
    public Flux<User> getAllByFilter(UserSearchFilter filter) {
        return userService.findAllByFilter(filter);
    }

    @PatchMapping("/{id}")
    public Mono<UserProfile> update(@PathVariable Long id, @RequestBody UserUpdateDto userUpdateDto) {
        return userService.updateProfile(id, userUpdateDto);
    }
}
