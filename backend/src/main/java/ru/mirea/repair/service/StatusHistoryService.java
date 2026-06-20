package ru.mirea.repair.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.mirea.repair.dto.StatusHistoryResponse;
import ru.mirea.repair.entity.RepairRequest;
import ru.mirea.repair.entity.RequestStatus;
import ru.mirea.repair.entity.StatusHistory;
import ru.mirea.repair.entity.User;
import ru.mirea.repair.repository.StatusHistoryRepository;

import java.util.List;

@Service
public class StatusHistoryService {
    private final StatusHistoryRepository statusHistoryRepository;
    private final RequestAccessService requestAccessService;

    public StatusHistoryService(StatusHistoryRepository statusHistoryRepository,
                                 RequestAccessService requestAccessService) {
        this.statusHistoryRepository = statusHistoryRepository;
        this.requestAccessService = requestAccessService;
    }

    @Transactional(readOnly = true)
    public List<StatusHistoryResponse> findAll(Long requestId, User currentUser) {
        RepairRequest request = requestAccessService.getRequestOrThrow(requestId);
        requestAccessService.checkReadAccess(request, currentUser);
        return statusHistoryRepository.findAllByRequestOrderByChangedAtAsc(request).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public void record(RepairRequest request, RequestStatus oldStatus, RequestStatus newStatus, User changedBy, String comment) {
        StatusHistory entry = new StatusHistory();
        entry.setRequest(request);
        entry.setOldStatus(oldStatus);
        entry.setNewStatus(newStatus);
        entry.setChangedBy(changedBy);
        entry.setComment(comment);
        statusHistoryRepository.save(entry);
    }

    private StatusHistoryResponse toResponse(StatusHistory entry) {
        return new StatusHistoryResponse(
                entry.getId(),
                entry.getRequest().getId(),
                entry.getChangedBy() == null ? null : entry.getChangedBy().getId(),
                entry.getChangedBy() == null ? null : entry.getChangedBy().getEmail(),
                entry.getOldStatus() == null ? null : entry.getOldStatus().name(),
                entry.getNewStatus().name(),
                entry.getComment(),
                entry.getChangedAt()
        );
    }
}
