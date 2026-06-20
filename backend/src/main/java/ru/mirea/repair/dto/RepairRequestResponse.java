package ru.mirea.repair.dto;

import java.time.LocalDateTime;

public record RepairRequestResponse(
        Long id,
        String title,
        String description,
        String equipmentType,
        String location,
        String priority,
        String status,
        Long userId,
        String userEmail,
        Long categoryId,
        String categoryName,
        Long assignedMasterId,
        String assignedMasterEmail,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}
