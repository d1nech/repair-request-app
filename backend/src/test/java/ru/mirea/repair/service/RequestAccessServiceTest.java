package ru.mirea.repair.service;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import ru.mirea.repair.entity.RepairRequest;
import ru.mirea.repair.entity.Role;
import ru.mirea.repair.entity.User;
import ru.mirea.repair.exception.ApiException;
import ru.mirea.repair.repository.RepairRequestRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

class RequestAccessServiceTest {
    private final RequestAccessService accessService = new RequestAccessService(mock(RepairRequestRepository.class));

    private User userWithRole(Long id, Role role) {
        User user = new User();
        user.setId(id);
        user.setEmail(role.name().toLowerCase() + "@example.com");
        user.setRole(role);
        return user;
    }

    private RepairRequest requestOwnedBy(User owner) {
        RepairRequest request = new RepairRequest();
        request.setId(100L);
        request.setUser(owner);
        return request;
    }

    @Test
    void ownerHasReadAndWriteAccess() {
        User owner = userWithRole(1L, Role.USER);
        RepairRequest request = requestOwnedBy(owner);

        accessService.checkReadAccess(request, owner);
        accessService.checkWriteAccess(request, owner);
    }

    @Test
    void strangerUserHasNoReadAccess() {
        User owner = userWithRole(1L, Role.USER);
        User stranger = userWithRole(2L, Role.USER);
        RepairRequest request = requestOwnedBy(owner);

        assertThatThrownBy(() -> accessService.checkReadAccess(request, stranger))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> assertThat(((ApiException) ex).getStatus()).isEqualTo(HttpStatus.FORBIDDEN));
    }

    @Test
    void adminAndOperatorHaveReadAccessToAnyRequest() {
        User owner = userWithRole(1L, Role.USER);
        RepairRequest request = requestOwnedBy(owner);

        accessService.checkReadAccess(request, userWithRole(2L, Role.ADMIN));
        accessService.checkReadAccess(request, userWithRole(3L, Role.OPERATOR));
    }

    @Test
    void assignedMasterHasReadAccessButUnassignedMasterDoesNot() {
        User owner = userWithRole(1L, Role.USER);
        User assignedMaster = userWithRole(4L, Role.MASTER);
        User otherMaster = userWithRole(5L, Role.MASTER);
        RepairRequest request = requestOwnedBy(owner);
        request.setAssignedMaster(assignedMaster);

        accessService.checkReadAccess(request, assignedMaster);
        assertThatThrownBy(() -> accessService.checkReadAccess(request, otherMaster))
                .isInstanceOf(ApiException.class);
    }

    @Test
    void onlyOwnerOrAdminHaveWriteAccess() {
        User owner = userWithRole(1L, Role.USER);
        RepairRequest request = requestOwnedBy(owner);

        accessService.checkWriteAccess(request, userWithRole(2L, Role.ADMIN));
        assertThatThrownBy(() -> accessService.checkWriteAccess(request, userWithRole(3L, Role.OPERATOR)))
                .isInstanceOf(ApiException.class);
        assertThatThrownBy(() -> accessService.checkWriteAccess(request, userWithRole(4L, Role.MASTER)))
                .isInstanceOf(ApiException.class);
    }
}
