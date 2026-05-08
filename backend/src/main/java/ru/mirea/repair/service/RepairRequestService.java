package ru.mirea.repair.service;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.mirea.repair.dto.RepairRequestCreateUpdateRequest;
import ru.mirea.repair.dto.RepairRequestResponse;
import ru.mirea.repair.entity.*;
import ru.mirea.repair.exception.ApiException;
import ru.mirea.repair.repository.RepairRequestRepository;
import ru.mirea.repair.repository.UserRepository;

import java.util.List;

@Service
public class RepairRequestService {
    private final RepairRequestRepository repairRequestRepository;
    private final UserRepository userRepository;

    public RepairRequestService(RepairRequestRepository repairRequestRepository, UserRepository userRepository) {
        this.repairRequestRepository = repairRequestRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public List<RepairRequestResponse> findAllForUser(User currentUser) {
        List<RepairRequest> requests = currentUser.getRole() == Role.ADMIN
                ? repairRequestRepository.findAllByOrderByCreatedAtDesc()
                : repairRequestRepository.findAllByUserOrderByCreatedAtDesc(currentUser);
        return requests.stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public RepairRequestResponse findOne(Long id, User currentUser) {
        RepairRequest request = findEntity(id);
        checkAccess(request, currentUser);
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
        return toResponse(repairRequestRepository.save(request));
    }

    @Transactional
    public RepairRequestResponse update(Long id, RepairRequestCreateUpdateRequest dto, User currentUser) {
        RepairRequest request = findEntity(id);
        checkAccess(request, currentUser);
        apply(dto, request);
        return toResponse(repairRequestRepository.save(request));
    }

    @Transactional
    public RepairRequestResponse updateStatus(Long id, RequestStatus status) {
        RepairRequest request = findEntity(id);
        request.setStatus(status);
        return toResponse(repairRequestRepository.save(request));
    }

    @Transactional
    public void delete(Long id, User currentUser) {
        RepairRequest request = findEntity(id);
        checkAccess(request, currentUser);
        repairRequestRepository.delete(request);
    }

    private RepairRequest findEntity(Long id) {
        return repairRequestRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Заявка не найдена"));
    }

    private void checkAccess(RepairRequest request, User currentUser) {
        boolean isOwner = request.getUser().getId().equals(currentUser.getId());
        boolean isAdmin = currentUser.getRole() == Role.ADMIN;
        if (!isOwner && !isAdmin) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Нет доступа к данной заявке");
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
                request.getCreatedAt(),
                request.getUpdatedAt()
        );
    }
}
