package org.example.chatapplication.Service;

import org.example.chatapplication.DTO.Request.AdminCreateUserRequest;
import org.example.chatapplication.DTO.Request.AdminUpdateUserRequest;
import org.example.chatapplication.DTO.Response.AdminUserDetailResponse;
import org.example.chatapplication.DTO.Response.AdminUserStatusResponse;
import org.example.chatapplication.Model.Enum.AdminAuditAction;
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

    @Mock
    private PasswordHasher passwordHasher;

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

    @Test
    void createUserShouldHashPasswordAndApplyDefaults() {
        AdminCreateUserRequest request = new AdminCreateUserRequest(
                "newuser",
                "New User",
                null,
                null,
                null,
                null,
                null,
                null,
                "new@example.com",
                null,
                "123456",
                null,
                null
        );

        when(userAccountRepository.findByUsernameIgnoreCase("newuser")).thenReturn(Optional.empty());
        when(passwordHasher.hash("123456")).thenReturn("hashed-password");
        when(userAccountRepository.save(any(UserAccount.class))).thenAnswer(invocation -> {
            UserAccount saved = invocation.getArgument(0);
            saved.setId(UUID.randomUUID());
            return saved;
        });

        AdminUserDetailResponse response = adminUserManagementService.createUser(request, "admin");

        assertThat(response.getUsername()).isEqualTo("newuser");
        assertThat(response.isAccountLocked()).isFalse();
        assertThat(response.getRole()).isEqualTo(UserRole.USER);
        verify(adminAuditService).log(eq(AdminAuditAction.USER_CREATED), eq("admin"), any(), any());
    }

    @Test
    void updateUserShouldRejectDuplicateUsername() {
        UUID targetUserId = UUID.randomUUID();
        UUID anotherUserId = UUID.randomUUID();
        UserAccount target = user(targetUserId, "target", UserRole.USER, false);
        UserAccount another = user(anotherUserId, "taken", UserRole.USER, false);
        when(userAccountRepository.findById(targetUserId)).thenReturn(Optional.of(target));
        when(userAccountRepository.findByUsernameIgnoreCase("taken")).thenReturn(Optional.of(another));

        AdminUpdateUserRequest request = new AdminUpdateUserRequest(
                "taken",
                "Target",
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null
        );

        assertThatThrownBy(() -> adminUserManagementService.updateUser(targetUserId, request, "admin"))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> {
                    ResponseStatusException responseException = (ResponseStatusException) ex;
                    assertThat(responseException.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
                });
    }

    @Test
    void deleteUserShouldRejectSelfDelete() {
        UUID userId = UUID.randomUUID();
        UserAccount admin = user(userId, "admin", UserRole.ADMIN, false);
        when(userAccountRepository.findById(userId)).thenReturn(Optional.of(admin));

        assertThatThrownBy(() -> adminUserManagementService.deleteUser(userId, "admin"))
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

