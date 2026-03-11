package ru.nvkz.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record RegistrationRequest(
        @NotBlank(message = "{user.email.notblank}")
        @Email(message = "{user.email.invalid}")
        String email,
        @Size(min = 8, message = "{user.password.size}")
        String password,
        @NotBlank(message = "{user.name.notblank}")
        String name,
        String surname,
        String phone,
        @Past(message = "{user.birthdate.past}")
        LocalDate birthdate
) {
}
