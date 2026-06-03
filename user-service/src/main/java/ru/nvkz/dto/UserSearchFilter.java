package ru.nvkz.dto;

public record UserSearchFilter(
    String email,
    String role,
    String namePart
) {}