package ru.mirea.repair.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import ru.mirea.repair.dto.AttachmentResponse;
import ru.mirea.repair.entity.RepairRequest;
import ru.mirea.repair.entity.RequestAttachment;
import ru.mirea.repair.entity.User;
import ru.mirea.repair.exception.ApiException;
import ru.mirea.repair.repository.RequestAttachmentRepository;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;

@Service
public class AttachmentService {
    private final RequestAttachmentRepository attachmentRepository;
    private final RequestAccessService requestAccessService;
    private final Path uploadRoot;

    public AttachmentService(RequestAttachmentRepository attachmentRepository,
                              RequestAccessService requestAccessService,
                              @Value("${app.upload.dir:uploads}") String uploadDir) {
        this.attachmentRepository = attachmentRepository;
        this.requestAccessService = requestAccessService;
        this.uploadRoot = Paths.get(uploadDir).toAbsolutePath().normalize();
    }

    @Transactional(readOnly = true)
    public List<AttachmentResponse> findAll(Long requestId, User currentUser) {
        RepairRequest request = requestAccessService.getRequestOrThrow(requestId);
        requestAccessService.checkReadAccess(request, currentUser);
        return attachmentRepository.findAllByRequestOrderByUploadedAtAsc(request).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public AttachmentResponse upload(Long requestId, MultipartFile file, User currentUser) {
        RepairRequest request = requestAccessService.getRequestOrThrow(requestId);
        requestAccessService.checkReadAccess(request, currentUser);
        if (file == null || file.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Файл не может быть пустым");
        }
        try {
            Path requestDir = uploadRoot.resolve(String.valueOf(requestId));
            Files.createDirectories(requestDir);
            String originalName = StringUtils.cleanPath(
                    file.getOriginalFilename() == null || file.getOriginalFilename().isBlank()
                            ? "file" : file.getOriginalFilename()
            );
            String storedName = UUID.randomUUID() + "_" + originalName;
            Path target = requestDir.resolve(storedName);
            file.transferTo(target);

            RequestAttachment attachment = new RequestAttachment();
            attachment.setRequest(request);
            attachment.setFileName(originalName);
            attachment.setFileUrl(requestId + "/" + storedName);
            attachment.setMimeType(file.getContentType() == null ? "application/octet-stream" : file.getContentType());
            return toResponse(attachmentRepository.save(attachment));
        } catch (IOException e) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Не удалось сохранить файл");
        }
    }

    @Transactional(readOnly = true)
    public Resource download(Long requestId, Long attachmentId, User currentUser) {
        RepairRequest request = requestAccessService.getRequestOrThrow(requestId);
        requestAccessService.checkReadAccess(request, currentUser);
        RequestAttachment attachment = getMeta(requestId, attachmentId);
        Path path = uploadRoot.resolve(attachment.getFileUrl()).normalize();
        if (!path.startsWith(uploadRoot)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Некорректный путь к файлу");
        }
        Resource resource = new FileSystemResource(path);
        if (!resource.exists()) {
            throw new ApiException(HttpStatus.NOT_FOUND, "Файл не найден на диске");
        }
        return resource;
    }

    @Transactional(readOnly = true)
    public RequestAttachment getMeta(Long requestId, Long attachmentId) {
        return attachmentRepository.findById(attachmentId)
                .filter(a -> a.getRequest().getId().equals(requestId))
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Файл не найден"));
    }

    private AttachmentResponse toResponse(RequestAttachment attachment) {
        return new AttachmentResponse(
                attachment.getId(),
                attachment.getRequest().getId(),
                attachment.getFileName(),
                attachment.getFileUrl(),
                attachment.getMimeType(),
                attachment.getUploadedAt()
        );
    }
}
