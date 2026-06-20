package ru.mirea.repair.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.mirea.repair.entity.RepairRequest;
import ru.mirea.repair.entity.StatusHistory;

import java.util.List;

public interface StatusHistoryRepository extends JpaRepository<StatusHistory, Long> {
    List<StatusHistory> findAllByRequestOrderByChangedAtAsc(RepairRequest request);
}
