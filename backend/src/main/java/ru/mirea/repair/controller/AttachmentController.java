package ru.mirea.repair.controller;

import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import ru.mirea.repair.dto.AttachmentResponse;
import ru.mirea.repair.entity.RequestAttachment;
import ru.mirea.repair.security.UserPrincipal;
import ru.mirea.repair.service.AttachmentService;

import java.util.List;

@RestController
@RequestMapping("/api/requests/{requestId}/attachments")
public class AttachmentController {
    private final AttachmentService attachmentService;

    public AttachmentController(AttachmentService attachmentService) {
        this.attachmentService = attachmentService;
    }

    @GetMapping
    public List<AttachmentResponse> findAll(@PathVariable Long requestId,
                                            @AuthenticationPrincipal UserPrincipal principal) {
        return attachmentService.findAll(requestId, principal.getUser());
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public AttachmentResponse upload(@PathVariable Long requestId,
                                     @RequestParam("file") MultipartFile file,
                                     @AuthenticationPrincipal UserPrincipal principal) {
        return attachmentService.upload(requestId, file, principal.getUser());
    }

    @GetMapping("/{attachmentId}/download")
    public ResponseEntity<Resource> download(@PathVariable Long requestId,
                                             @PathVariable Long attachmentId,
                                             @AuthenticationPrincipal UserPrincipal principal) {
        Resource resource = attachmentService.download(requestId, attachmentId, principal.getUser());
        RequestAttachment meta = attachmentService.getMeta(requestId, attachmentId);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(meta.getMimeType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + meta.getFileName() + "\"")
                .body(resource);
    }
}
