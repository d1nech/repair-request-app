package ru.mirea.repair.controller;

import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import ru.mirea.repair.dto.AssignMasterRequest;
import ru.mirea.repair.dto.ClassifyRequest;
import ru.mirea.repair.dto.RepairRequestCreateUpdateRequest;
import ru.mirea.repair.dto.RepairRequestResponse;
import ru.mirea.repair.entity.RequestStatus;
import ru.mirea.repair.security.UserPrincipal;
import ru.mirea.repair.service.RepairRequestService;

import java.util.List;

@RestController
@RequestMapping("/api/requests")
public class RepairRequestController {
    private final RepairRequestService repairRequestService;

    public RepairRequestController(RepairRequestService repairRequestService) {
        this.repairRequestService = repairRequestService;
    }

    @GetMapping
    public List<RepairRequestResponse> findAll(@AuthenticationPrincipal UserPrincipal principal) {
        return repairRequestService.findAllForUser(principal.getUser());
    }

    @GetMapping("/{id}")
    public RepairRequestResponse findOne(@PathVariable Long id,
                                         @AuthenticationPrincipal UserPrincipal principal) {
        return repairRequestService.findOne(id, principal.getUser());
    }

    @PostMapping
    public RepairRequestResponse create(@Valid @RequestBody RepairRequestCreateUpdateRequest request,
                                        @AuthenticationPrincipal UserPrincipal principal) {
        return repairRequestService.create(request, principal.getId());
    }

    @PutMapping("/{id}")
    public RepairRequestResponse update(@PathVariable Long id,
                                        @Valid @RequestBody RepairRequestCreateUpdateRequest request,
                                        @AuthenticationPrincipal UserPrincipal principal) {
        return repairRequestService.update(id, request, principal.getUser());
    }

    @PreAuthorize("hasAnyRole('ADMIN','OPERATOR','MASTER')")
    @PatchMapping("/{id}/status")
    public RepairRequestResponse updateStatus(@PathVariable Long id,
                                              @RequestParam RequestStatus status,
                                              @RequestParam(required = false) String comment,
                                              @AuthenticationPrincipal UserPrincipal principal) {
        return repairRequestService.updateStatus(id, status, principal.getUser(), comment);
    }

    @PreAuthorize("hasAnyRole('ADMIN','OPERATOR')")
    @PatchMapping("/{id}/assign")
    public RepairRequestResponse assignMaster(@PathVariable Long id,
                                              @Valid @RequestBody AssignMasterRequest request) {
        return repairRequestService.assignMaster(id, request.masterId());
    }

    @PreAuthorize("hasAnyRole('ADMIN','OPERATOR')")
    @PatchMapping("/{id}/classify")
    public RepairRequestResponse classify(@PathVariable Long id,
                                          @Valid @RequestBody ClassifyRequest request) {
        return repairRequestService.classify(id, request.categoryId());
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id,
                       @AuthenticationPrincipal UserPrincipal principal) {
        repairRequestService.delete(id, principal.getUser());
    }
}
