package ru.mirea.repair.service;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.mirea.repair.dto.RepairRequestCreateUpdateRequest;
import ru.mirea.repair.dto.RepairRequestResponse;
import ru.mirea.repair.entity.*;
import ru.mirea.repair.exception.ApiException;
import ru.mirea.repair.repository.CategoryRepository;
import ru.mirea.repair.repository.RepairRequestRepository;
import ru.mirea.repair.repository.UserRepository;

import java.util.List;

@Service
public class RepairRequestService {
    private final RepairRequestRepository repairRequestRepository;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final RequestAccessService requestAccessService;
    private final StatusHistoryService statusHistoryService;

    public RepairRequestService(RepairRequestRepository repairRequestRepository,
                                 UserRepository userRepository,
                                 CategoryRepository categoryRepository,
                                 RequestAccessService requestAccessService,
                                 StatusHistoryService statusHistoryService) {
        this.repairRequestRepository = repairRequestRepository;
        this.userRepository = userRepository;
        this.categoryRepository = categoryRepository;
        this.requestAccessService = requestAccessService;
        this.statusHistoryService = statusHistoryService;
    }

    @Transactional(readOnly = true)
    public List<RepairRequestResponse> findAllForUser(User currentUser) {
        List<RepairRequest> requests = switch (currentUser.getRole()) {
            case ADMIN, OPERATOR -> repairRequestRepository.findAllByOrderByCreatedAtDesc();
            case MASTER -> repairRequestRepository.findAllByAssignedMasterOrderByCreatedAtDesc(currentUser);
            case USER -> repairRequestRepository.findAllByUserOrderByCreatedAtDesc(currentUser);
        };
        return requests.stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public RepairRequestResponse findOne(Long id, User currentUser) {
        RepairRequest request = findEntity(id);
        requestAccessService.checkReadAccess(request, currentUser);
        return toResponse(request);
    }

    @Transactional
    public RepairRequestResponse create(RepairRequestCreateUpdateRequest dto, Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Пользователь не найден"));
        RepairRequest request = new RepairRequest();
        apply(dto, request);
        request.setStatus(RequestStatus.NEW);
        request.setUser(user);
        RepairRequest saved = repairRequestRepository.save(request);
        statusHistoryService.record(saved, null, RequestStatus.NEW, user, null);
        return toResponse(saved);
    }

    @Transactional
    public RepairRequestResponse update(Long id, RepairRequestCreateUpdateRequest dto, User currentUser) {
        RepairRequest request = findEntity(id);
        requestAccessService.checkWriteAccess(request, currentUser);
        apply(dto, request);
        return toResponse(repairRequestRepository.save(request));
    }

    @Transactional
    public RepairRequestResponse updateStatus(Long id, RequestStatus status, User currentUser, String comment) {
        RepairRequest request = findEntity(id);
        checkStatusChangeAccess(request, currentUser);
        RequestStatus oldStatus = request.getStatus();
        request.setStatus(status);
        RepairRequest saved = repairRequestRepository.save(request);
        statusHistoryService.record(saved, oldStatus, status, currentUser, comment);
        return toResponse(saved);
    }

    @Transactional
    public RepairRequestResponse assignMaster(Long id, Long masterId) {
        RepairRequest request = findEntity(id);
        User master = userRepository.findById(masterId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Мастер не найден"));
        if (master.getRole() != Role.MASTER) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Указанный пользователь не является мастером");
        }
        request.setAssignedMaster(master);
        return toResponse(repairRequestRepository.save(request));
    }

    @Transactional
    public RepairRequestResponse classify(Long id, Long categoryId) {
        RepairRequest request = findEntity(id);
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Категория не найдена"));
        request.setCategory(category);
        return toResponse(repairRequestRepository.save(request));
    }

    @Transactional
    public void delete(Long id, User currentUser) {
        RepairRequest request = findEntity(id);
        requestAccessService.checkWriteAccess(request, currentUser);
        repairRequestRepository.delete(request);
    }

    private RepairRequest findEntity(Long id) {
        return requestAccessService.getRequestOrThrow(id);
    }

    private void checkStatusChangeAccess(RepairRequest request, User currentUser) {
        boolean allowed = switch (currentUser.getRole()) {
            case ADMIN, OPERATOR -> true;
            case MASTER -> request.getAssignedMaster() != null
                    && request.getAssignedMaster().getId().equals(currentUser.getId());
            case USER -> false;
        };
        if (!allowed) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Нет прав на изменение статуса заявки");
        }
    }

    private void apply(RepairRequestCreateUpdateRequest dto, RepairRequest request) {
        request.setTitle(dto.title().trim());
        request.setDescription(dto.description().trim());
        request.setEquipmentType(dto.equipmentType().trim());
        request.setLocation(dto.location().trim());
        request.setPriority(dto.priority());
    }

    private RepairRequestResponse toResponse(RepairRequest request) {
        Category category = request.getCategory();
        User assignedMaster = request.getAssignedMaster();
        return new RepairRequestResponse(
                request.getId(),
                request.getTitle(),
                request.getDescription(),
                request.getEquipmentType(),
                request.getLocation(),
                request.getPriority().name(),
                request.getStatus().name(),
                request.getUser().getId(),
                request.getUser().getEmail(),
                category == null ? null : category.getId(),
                category == null ? null : category.getName(),
                assignedMaster == null ? null : assignedMaster.getId(),
                assignedMaster == null ? null : assignedMaster.getEmail(),
                request.getCreatedAt(),
                request.getUpdatedAt()
        );
    }
}
