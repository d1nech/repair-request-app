package ru.mirea.repair.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.mirea.repair.entity.RepairRequest;
import ru.mirea.repair.entity.RequestAttachment;

import java.util.List;

public interface RequestAttachmentRepository extends JpaRepository<RequestAttachment, Long> {
    List<RequestAttachment> findAllByRequestOrderByUploadedAtAsc(RepairRequest request);
}
