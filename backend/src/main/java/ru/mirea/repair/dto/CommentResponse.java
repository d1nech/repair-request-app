package ru.mirea.repair.dto;

import java.time.LocalDateTime;

public record CommentResponse(
        Long id,
        Long requestId,
        Long authorId,
        String authorEmail,
        String message,
        LocalDateTime createdAt
) {}
