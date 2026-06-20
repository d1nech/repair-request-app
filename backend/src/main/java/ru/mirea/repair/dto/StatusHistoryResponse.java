package ru.mirea.repair.dto;

import java.time.LocalDateTime;

public record StatusHistoryResponse(
        Long id,
        Long requestId,
        Long changedById,
        String changedByEmail,
        String oldStatus,
        String newStatus,
        String comment,
        LocalDateTime changedAt
) {}
