package ru.nvkz.dto;

public record UserSearchRequest(
    String email,
    String role,
    String namePart
) {}