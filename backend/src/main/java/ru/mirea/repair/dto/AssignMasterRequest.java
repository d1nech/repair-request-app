package ru.mirea.repair.dto;

import jakarta.validation.constraints.NotNull;

public record AssignMasterRequest(
        @NotNull(message = "Идентификатор мастера обязателен")
        Long masterId
) {}
