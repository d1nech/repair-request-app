package ru.mirea.repair.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.mirea.repair.entity.RepairRequest;
import ru.mirea.repair.entity.RequestComment;

import java.util.List;

public interface RequestCommentRepository extends JpaRepository<RequestComment, Long> {
    List<RequestComment> findAllByRequestOrderByCreatedAtAsc(RepairRequest request);
}
