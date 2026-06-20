package ru.mirea.repair.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CommentRequest(
        @NotBlank(message = "Комментарий не может быть пустым")
        @Size(min = 1, max = 2000, message = "Комментарий должен содержать от 1 до 2000 символов")
        String message
) {}
