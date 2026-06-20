package ru.mirea.repair.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CategoryRequest(
        @NotBlank(message = "Название категории обязательно")
        @Size(min = 2, max = 120, message = "Название должно содержать от 2 до 120 символов")
        String name,

        @Size(max = 500, message = "Описание не должно превышать 500 символов")
        String description
) {}
