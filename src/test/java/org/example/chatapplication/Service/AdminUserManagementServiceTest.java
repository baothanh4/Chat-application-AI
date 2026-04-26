package org.example.chatapplication.Service;

import org.example.chatapplication.DTO.Response.AdminUserStatusResponse;
import org.example.chatapplication.Model.Entity.UserAccount;
import org.example.chatapplication.Model.Enum.UserRole;
import org.example.chatapplication.Repository.UserAccountRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminUserManagementServiceTest {

    @Mock
    private UserAccountRepository userAccountRepository;

    @Mock
    private AdminAuditService adminAuditService;

    @InjectMocks
    private AdminUserManagementService adminUserManagementService;

    @Test
    void lockUserShouldRejectSelfLock() {
        UUID userId = UUID.randomUUID();
        UserAccount admin = user(userId, "admin", UserRole.ADMIN, false);
        when(userAccountRepository.findById(userId)).thenReturn(Optional.of(admin));

        assertThatThrownBy(() -> adminUserManagementService.lockUser(userId, "admin"))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> {
                    ResponseStatusException responseException = (ResponseStatusException) ex;
                    assertThat(responseException.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
                });
    }

    @Test
    void unlockUserShouldReturnUnlockedStatus() {
        UUID userId = UUID.randomUUID();
        UserAccount account = user(userId, "member", UserRole.USER, true);
        when(userAccountRepository.findById(userId)).thenReturn(Optional.of(account));
        when(userAccountRepository.save(any(UserAccount.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AdminUserStatusResponse response = adminUserManagementService.unlockUser(userId, "admin");

        assertThat(response.isAccountLocked()).isFalse();
        assertThat(response.getUserId()).isEqualTo(userId);
        assertThat(response.getMessage()).contains("unlocked");
        verify(adminAuditService).log(eq(org.example.chatapplication.Model.Enum.AdminAuditAction.USER_UNLOCKED), eq("admin"), eq(userId), any());
    }

    @Test
    void updateRoleShouldRejectSelfRoleChange() {
        UUID userId = UUID.randomUUID();
        UserAccount admin = user(userId, "admin", UserRole.ADMIN, false);
        when(userAccountRepository.findById(userId)).thenReturn(Optional.of(admin));

        assertThatThrownBy(() -> adminUserManagementService.updateUserRole(userId, UserRole.MODERATOR, "admin"))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> {
                    ResponseStatusException responseException = (ResponseStatusException) ex;
                    assertThat(responseException.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
                });
    }

    private UserAccount user(UUID id, String username, UserRole role, boolean locked) {
        UserAccount user = new UserAccount();
        user.setId(id);
        user.setUsername(username);
        user.setRole(role);
        user.setAccountLocked(locked);
        return user;
    }
}

