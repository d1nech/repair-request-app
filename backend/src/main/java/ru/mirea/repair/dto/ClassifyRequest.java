package ru.mirea.repair.dto;

import jakarta.validation.constraints.NotNull;

public record ClassifyRequest(
        @NotNull(message = "Идентификатор категории обязателен")
        Long categoryId
) {}
