package ru.nvkz.dto;

import java.time.LocalDate;

public record UserUpdateDto(
        String name,
        String surname,
        String phone,
        LocalDate birthdate
) {
}
