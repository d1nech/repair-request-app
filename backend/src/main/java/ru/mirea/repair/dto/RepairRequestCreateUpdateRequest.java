package ru.mirea.repair.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import ru.mirea.repair.entity.RequestPriority;

public record RepairRequestCreateUpdateRequest(
        @NotBlank(message = "Заголовок заявки обязателен")
        @Size(min = 3, max = 160, message = "Заголовок должен содержать от 3 до 160 символов")
        String title,

        @NotBlank(message = "Описание заявки обязательно")
        @Size(min = 10, max = 2000, message = "Описание должно содержать от 10 до 2000 символов")
        String description,

        @NotBlank(message = "Тип оборудования обязателен")
        @Size(min = 2, max = 120, message = "Тип оборудования должен содержать от 2 до 120 символов")
        String equipmentType,

        @NotBlank(message = "Место выполнения ремонта обязательно")
        @Size(min = 2, max = 200, message = "Местоположение должно содержать от 2 до 200 символов")
        String location,

        @NotNull(message = "Приоритет обязателен")
        RequestPriority priority
) {}
