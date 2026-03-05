package ru.nvkz.dto;


import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegistrationRequest(
        @NotBlank(message = "Email не может быть пустым")
        @Email(message = "Неверный формат email")
        String email,
        @Size(min = 8, message = "Пароль должен быть не менее 8 символов")
        String password,
        @NotBlank(message = "Имя обязательно")
        String name,
        String surname
) {
}
