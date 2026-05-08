package ru.mirea.repair.dto;

import java.time.LocalDateTime;

public record UserResponse(
        Long id,
        String email,
        String fullName,
        String role,
        LocalDateTime createdAt
) {}
