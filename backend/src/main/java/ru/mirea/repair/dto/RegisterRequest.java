package ru.mirea.repair.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @Email(message = "Email должен иметь корректный формат")
        @NotBlank(message = "Email обязателен")
        String email,

        @NotBlank(message = "ФИО обязательно")
        @Size(min = 2, max = 160, message = "ФИО должно содержать от 2 до 160 символов")
        String fullName,

        @NotBlank(message = "Пароль обязателен")
        @Size(min = 8, max = 72, message = "Пароль должен содержать от 8 до 72 символов")
        String password
) {}
