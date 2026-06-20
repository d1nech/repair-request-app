package ru.mirea.repair.dto;

import java.time.LocalDateTime;

public record AttachmentResponse(
        Long id,
        Long requestId,
        String fileName,
        String fileUrl,
        String mimeType,
        LocalDateTime uploadedAt
) {}
