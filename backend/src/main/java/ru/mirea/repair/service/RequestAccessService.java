package ru.mirea.repair.service;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import ru.mirea.repair.entity.RepairRequest;
import ru.mirea.repair.entity.Role;
import ru.mirea.repair.entity.User;
import ru.mirea.repair.exception.ApiException;
import ru.mirea.repair.repository.RepairRequestRepository;

@Service
public class RequestAccessService {
    private final RepairRequestRepository repairRequestRepository;

    public RequestAccessService(RepairRequestRepository repairRequestRepository) {
        this.repairRequestRepository = repairRequestRepository;
    }

    public RepairRequest getRequestOrThrow(Long id) {
        return repairRequestRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Заявка не найдена"));
    }

    public void checkReadAccess(RepairRequest request, User currentUser) {
        boolean isOwner = request.getUser().getId().equals(currentUser.getId());
        boolean isStaff = currentUser.getRole() == Role.ADMIN || currentUser.getRole() == Role.OPERATOR;
        boolean isAssignedMaster = request.getAssignedMaster() != null
                && request.getAssignedMaster().getId().equals(currentUser.getId());
        if (!isOwner && !isStaff && !isAssignedMaster) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Нет доступа к данной заявке");
        }
    }

    public void checkWriteAccess(RepairRequest request, User currentUser) {
        boolean isOwner = request.getUser().getId().equals(currentUser.getId());
        boolean isAdmin = currentUser.getRole() == Role.ADMIN;
        if (!isOwner && !isAdmin) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Нет доступа к данной заявке");
        }
    }
}
