package ru.mirea.repair.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import ru.mirea.repair.dto.RepairRequestCreateUpdateRequest;
import ru.mirea.repair.entity.*;
import ru.mirea.repair.exception.ApiException;
import ru.mirea.repair.repository.CategoryRepository;
import ru.mirea.repair.repository.RepairRequestRepository;
import ru.mirea.repair.repository.UserRepository;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RepairRequestServiceTest {
    @Mock
    private RepairRequestRepository repairRequestRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private CategoryRepository categoryRepository;
    @Mock
    private RequestAccessService requestAccessService;
    @Mock
    private StatusHistoryService statusHistoryService;

    private RepairRequestService service;

    @BeforeEach
    void setUp() {
        service = new RepairRequestService(
                repairRequestRepository, userRepository, categoryRepository,
                requestAccessService, statusHistoryService
        );
    }

    private User userWithRole(Long id, Role role) {
        User user = new User();
        user.setId(id);
        user.setEmail(role.name().toLowerCase() + "@example.com");
        user.setRole(role);
        return user;
    }

    private RepairRequest sampleRequest() {
        RepairRequest request = new RepairRequest();
        request.setId(10L);
        request.setUser(userWithRole(1L, Role.USER));
        request.setStatus(RequestStatus.NEW);
        request.setPriority(RequestPriority.MEDIUM);
        return request;
    }

    @Test
    void findAllForUser_dispatchesByRole() {
        User admin = userWithRole(2L, Role.ADMIN);
        User operator = userWithRole(3L, Role.OPERATOR);
        User master = userWithRole(4L, Role.MASTER);
        User client = userWithRole(1L, Role.USER);

        when(repairRequestRepository.findAllByOrderByCreatedAtDesc()).thenReturn(List.of());
        when(repairRequestRepository.findAllByAssignedMasterOrderByCreatedAtDesc(master)).thenReturn(List.of());
        when(repairRequestRepository.findAllByUserOrderByCreatedAtDesc(client)).thenReturn(List.of());

        service.findAllForUser(admin);
        service.findAllForUser(operator);
        service.findAllForUser(master);
        service.findAllForUser(client);

        verify(repairRequestRepository, times(2)).findAllByOrderByCreatedAtDesc();
        verify(repairRequestRepository).findAllByAssignedMasterOrderByCreatedAtDesc(master);
        verify(repairRequestRepository).findAllByUserOrderByCreatedAtDesc(client);
    }

    @Test
    void updateStatus_allowedForAdminAndOperator() {
        RepairRequest request = sampleRequest();
        when(requestAccessService.getRequestOrThrow(10L)).thenReturn(request);
        when(repairRequestRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        service.updateStatus(10L, RequestStatus.IN_PROGRESS, userWithRole(2L, Role.ADMIN), null);
        service.updateStatus(10L, RequestStatus.IN_PROGRESS, userWithRole(3L, Role.OPERATOR), null);

        verify(statusHistoryService, times(2)).record(eq(request), any(), eq(RequestStatus.IN_PROGRESS), any(), any());
    }

    @Test
    void updateStatus_forbiddenForClient() {
        RepairRequest request = sampleRequest();
        when(requestAccessService.getRequestOrThrow(10L)).thenReturn(request);

        assertThatThrownBy(() -> service.updateStatus(10L, RequestStatus.DONE, userWithRole(1L, Role.USER), null))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> assertThat(((ApiException) ex).getStatus()).isEqualTo(HttpStatus.FORBIDDEN));

        verifyNoInteractions(statusHistoryService);
    }

    @Test
    void updateStatus_allowedForAssignedMasterOnly() {
        RepairRequest request = sampleRequest();
        User assignedMaster = userWithRole(4L, Role.MASTER);
        User otherMaster = userWithRole(5L, Role.MASTER);
        request.setAssignedMaster(assignedMaster);
        when(requestAccessService.getRequestOrThrow(10L)).thenReturn(request);
        when(repairRequestRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        service.updateStatus(10L, RequestStatus.DONE, assignedMaster, "Готово");
        verify(statusHistoryService).record(eq(request), any(), eq(RequestStatus.DONE), eq(assignedMaster), eq("Готово"));

        assertThatThrownBy(() -> service.updateStatus(10L, RequestStatus.DONE, otherMaster, null))
                .isInstanceOf(ApiException.class);
    }

    @Test
    void assignMaster_rejectsUserWithoutMasterRole() {
        RepairRequest request = sampleRequest();
        User notAMaster = userWithRole(6L, Role.USER);
        when(requestAccessService.getRequestOrThrow(10L)).thenReturn(request);
        when(userRepository.findById(6L)).thenReturn(Optional.of(notAMaster));

        assertThatThrownBy(() -> service.assignMaster(10L, 6L))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> assertThat(((ApiException) ex).getStatus()).isEqualTo(HttpStatus.BAD_REQUEST));

        verify(repairRequestRepository, never()).save(any());
    }

    @Test
    void assignMaster_succeedsForMasterRole() {
        RepairRequest request = sampleRequest();
        User master = userWithRole(7L, Role.MASTER);
        when(requestAccessService.getRequestOrThrow(10L)).thenReturn(request);
        when(userRepository.findById(7L)).thenReturn(Optional.of(master));
        when(repairRequestRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        service.assignMaster(10L, 7L);

        assertThat(request.getAssignedMaster()).isEqualTo(master);
    }

    @Test
    void classify_throwsNotFoundForMissingCategory() {
        RepairRequest request = sampleRequest();
        when(requestAccessService.getRequestOrThrow(10L)).thenReturn(request);
        when(categoryRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.classify(10L, 99L))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> assertThat(((ApiException) ex).getStatus()).isEqualTo(HttpStatus.NOT_FOUND));
    }

    @Test
    void create_setsNewStatusAndRecordsHistory() {
        User client = userWithRole(1L, Role.USER);
        when(userRepository.findById(1L)).thenReturn(Optional.of(client));
        when(repairRequestRepository.save(any())).thenAnswer(invocation -> {
            RepairRequest saved = invocation.getArgument(0);
            saved.setId(50L);
            return saved;
        });

        RepairRequestCreateUpdateRequest dto = new RepairRequestCreateUpdateRequest(
                "Заголовок заявки", "Подробное описание проблемы", "Принтер", "Кабинет 1", RequestPriority.LOW
        );

        var response = service.create(dto, 1L);

        assertThat(response.status()).isEqualTo(RequestStatus.NEW.name());
        verify(statusHistoryService).record(any(), isNull(), eq(RequestStatus.NEW), eq(client), isNull());
    }
}
