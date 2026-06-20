package ru.mirea.repair.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.mirea.repair.dto.StatusHistoryResponse;
import ru.mirea.repair.security.UserPrincipal;
import ru.mirea.repair.service.StatusHistoryService;

import java.util.List;

@RestController
@RequestMapping("/api/requests/{requestId}/status-history")
public class StatusHistoryController {
    private final StatusHistoryService statusHistoryService;

    public StatusHistoryController(StatusHistoryService statusHistoryService) {
        this.statusHistoryService = statusHistoryService;
    }

    @GetMapping
    public List<StatusHistoryResponse> findAll(@PathVariable Long requestId,
                                               @AuthenticationPrincipal UserPrincipal principal) {
        return statusHistoryService.findAll(requestId, principal.getUser());
    }
}
