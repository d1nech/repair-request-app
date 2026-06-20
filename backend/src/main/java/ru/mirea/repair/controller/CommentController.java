package ru.mirea.repair.controller;

import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import ru.mirea.repair.dto.CommentRequest;
import ru.mirea.repair.dto.CommentResponse;
import ru.mirea.repair.security.UserPrincipal;
import ru.mirea.repair.service.CommentService;

import java.util.List;

@RestController
@RequestMapping("/api/requests/{requestId}/comments")
public class CommentController {
    private final CommentService commentService;

    public CommentController(CommentService commentService) {
        this.commentService = commentService;
    }

    @GetMapping
    public List<CommentResponse> findAll(@PathVariable Long requestId,
                                         @AuthenticationPrincipal UserPrincipal principal) {
        return commentService.findAll(requestId, principal.getUser());
    }

    @PostMapping
    public CommentResponse add(@PathVariable Long requestId,
                               @Valid @RequestBody CommentRequest request,
                               @AuthenticationPrincipal UserPrincipal principal) {
        return commentService.add(requestId, request, principal.getUser());
    }
}
