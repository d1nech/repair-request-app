package ru.mirea.repair.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.mirea.repair.entity.RepairRequest;
import ru.mirea.repair.entity.User;

import java.util.List;

public interface RepairRequestRepository extends JpaRepository<RepairRequest, Long> {
    List<RepairRequest> findAllByUserOrderByCreatedAtDesc(User user);
    List<RepairRequest> findAllByOrderByCreatedAtDesc();
    List<RepairRequest> findAllByAssignedMasterOrderByCreatedAtDesc(User assignedMaster);
}
