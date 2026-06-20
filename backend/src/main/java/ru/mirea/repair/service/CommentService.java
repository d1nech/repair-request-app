package ru.mirea.repair.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.mirea.repair.dto.CommentRequest;
import ru.mirea.repair.dto.CommentResponse;
import ru.mirea.repair.entity.RepairRequest;
import ru.mirea.repair.entity.RequestComment;
import ru.mirea.repair.entity.User;
import ru.mirea.repair.repository.RequestCommentRepository;

import java.util.List;

@Service
public class CommentService {
    private final RequestCommentRepository commentRepository;
    private final RequestAccessService requestAccessService;

    public CommentService(RequestCommentRepository commentRepository, RequestAccessService requestAccessService) {
        this.commentRepository = commentRepository;
        this.requestAccessService = requestAccessService;
    }

    @Transactional(readOnly = true)
    public List<CommentResponse> findAll(Long requestId, User currentUser) {
        RepairRequest request = requestAccessService.getRequestOrThrow(requestId);
        requestAccessService.checkReadAccess(request, currentUser);
        return commentRepository.findAllByRequestOrderByCreatedAtAsc(request).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public CommentResponse add(Long requestId, CommentRequest dto, User currentUser) {
        RepairRequest request = requestAccessService.getRequestOrThrow(requestId);
        requestAccessService.checkReadAccess(request, currentUser);
        RequestComment comment = new RequestComment();
        comment.setRequest(request);
        comment.setAuthor(currentUser);
        comment.setMessage(dto.message().trim());
        return toResponse(commentRepository.save(comment));
    }

    private CommentResponse toResponse(RequestComment comment) {
        return new CommentResponse(
                comment.getId(),
                comment.getRequest().getId(),
                comment.getAuthor().getId(),
                comment.getAuthor().getEmail(),
                comment.getMessage(),
                comment.getCreatedAt()
        );
    }
}
