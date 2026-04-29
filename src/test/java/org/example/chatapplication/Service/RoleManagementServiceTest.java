package org.example.chatapplication.Service;

import org.example.chatapplication.DTO.Request.AssignRoleRequest;
import org.example.chatapplication.DTO.Request.CreateRoleRequest;
import org.example.chatapplication.DTO.Response.RoleResponse;
import org.example.chatapplication.DTO.Response.UserRolesResponse;
import org.example.chatapplication.Model.Entity.Role;
import org.example.chatapplication.Model.Entity.UserAccount;
import org.example.chatapplication.Model.Entity.UserRoleAssignment;
import org.example.chatapplication.Model.Enum.AdminAuditAction;
import org.example.chatapplication.Repository.RoleRepository;
import org.example.chatapplication.Repository.UserAccountRepository;
import org.example.chatapplication.Repository.UserRoleAssignmentRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RoleManagementServiceTest {

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private UserAccountRepository userAccountRepository;

    @Mock
    private UserRoleAssignmentRepository userRoleAssignmentRepository;

    @Mock
    private AdminAuditService adminAuditService;

    @InjectMocks
    private RoleManagementService roleManagementService;

    @Test
    void createRoleShouldCreateAndReturnRole() {
        UUID roleId = UUID.randomUUID();
        CreateRoleRequest request = new CreateRoleRequest("MODERATOR", "Can moderate content");

        Role role = new Role();
        role.setId(roleId);
        role.setName("MODERATOR");
        role.setDescription("Can moderate content");
        role.setBuiltIn(false);

        when(roleRepository.findByNameIgnoreCase("MODERATOR")).thenReturn(Optional.empty());
        when(roleRepository.save(any(Role.class))).thenReturn(role);

        RoleResponse response = roleManagementService.createRole(request, "admin");

        assertThat(response.getName()).isEqualTo("MODERATOR");
        assertThat(response.isBuiltIn()).isFalse();
        verify(adminAuditService).log(eq(AdminAuditAction.ROLE_CREATED), eq("admin"), any(), any());
    }

    @Test
    void createRoleShouldFailIfNameExists() {
        CreateRoleRequest request = new CreateRoleRequest("MODERATOR", "Can moderate");

        Role existing = new Role("MODERATOR", "Existing", false);
        when(roleRepository.findByNameIgnoreCase("MODERATOR")).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> roleManagementService.createRole(request, "admin"))
                .isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void updateRoleShouldUpdateAndReturnRole() {
        UUID roleId = UUID.randomUUID();
        CreateRoleRequest request = new CreateRoleRequest("UPDATED_MODERATOR", "Updated description");

        Role role = new Role("MODERATOR", "Original", false);
        role.setId(roleId);

        when(roleRepository.findById(roleId)).thenReturn(Optional.of(role));
        when(roleRepository.findByNameIgnoreCase("UPDATED_MODERATOR")).thenReturn(Optional.empty());
        when(roleRepository.save(any(Role.class))).thenAnswer(invocation -> invocation.getArgument(0));

        RoleResponse response = roleManagementService.updateRole(roleId, request, "admin");

        assertThat(response.getName()).isEqualTo("UPDATED_MODERATOR");
        verify(adminAuditService).log(eq(AdminAuditAction.ROLE_UPDATED), eq("admin"), any(), any());
    }

    @Test
    void updateRoleShouldRejectIfBuiltIn() {
        UUID roleId = UUID.randomUUID();
        CreateRoleRequest request = new CreateRoleRequest("ADMIN", "Updated");

        Role builtInRole = new Role("ADMIN", "Built in", true);
        builtInRole.setId(roleId);

        when(roleRepository.findById(roleId)).thenReturn(Optional.of(builtInRole));

        assertThatThrownBy(() -> roleManagementService.updateRole(roleId, request, "admin"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Cannot modify built-in role");
    }

    @Test
    void deleteRoleShouldFailIfBuiltIn() {
        UUID roleId = UUID.randomUUID();

        Role builtInRole = new Role("ADMIN", "Built in", true);
        builtInRole.setId(roleId);

        when(roleRepository.findById(roleId)).thenReturn(Optional.of(builtInRole));

        assertThatThrownBy(() -> roleManagementService.deleteRole(roleId, "admin"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Cannot delete built-in role");
    }

    @Test
    void assignRoleToUserShouldSucceed() {
        UUID userId = UUID.randomUUID();
        UUID roleId = UUID.randomUUID();
        AssignRoleRequest request = new AssignRoleRequest(roleId);

        UserAccount user = mock(UserAccount.class);
        user.setId(userId);
        user.setUsername("testuser");

        Role role = new Role("MODERATOR", "Can moderate content", false);
        role.setId(roleId);

        when(userAccountRepository.findById(userId)).thenReturn(Optional.of(user));
        when(roleRepository.findById(roleId)).thenReturn(Optional.of(role));
        when(userRoleAssignmentRepository.findByUserId(userId)).thenReturn(List.of());
        when(userRoleAssignmentRepository.save(any(UserRoleAssignment.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        UserRolesResponse response = roleManagementService.assignRoleToUser(userId, request, "admin");

        assertThat(response.getUserId()).isEqualTo(userId);
        verify(adminAuditService).log(eq(AdminAuditAction.ROLE_ASSIGNED), eq("admin"), any(), any());
    }

    @Test
    void assignRoleToUserShouldFailIfAlreadyAssigned() {
        UUID userId = UUID.randomUUID();
        UUID roleId = UUID.randomUUID();
        AssignRoleRequest request = new AssignRoleRequest(roleId);

        UserAccount user = mock(UserAccount.class);
        user.setId(userId);

        Role role = new Role("MODERATOR", "Test role", false);
        role.setId(roleId);

        UserRoleAssignment existing = new UserRoleAssignment();
        existing.setRole(role);

        when(userAccountRepository.findById(userId)).thenReturn(Optional.of(user));
        when(roleRepository.findById(roleId)).thenReturn(Optional.of(role));
        when(userRoleAssignmentRepository.findByUserId(userId)).thenReturn(List.of(existing));

        assertThatThrownBy(() -> roleManagementService.assignRoleToUser(userId, request, "admin"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("User already has this role");
    }

    @Test
    void removeRoleFromUserShouldSucceed() {
        UUID userId = UUID.randomUUID();
        UUID roleId = UUID.randomUUID();

        UserAccount user = mock(UserAccount.class);
        user.setId(userId);
        user.setUsername("testuser");

        Role role = new Role("MODERATOR", "Can moderate content", false);
        role.setId(roleId);

        when(userAccountRepository.findById(userId)).thenReturn(Optional.of(user));
        when(roleRepository.findById(roleId)).thenReturn(Optional.of(role));

        roleManagementService.removeRoleFromUser(userId, roleId, "admin");

        verify(userRoleAssignmentRepository).deleteByUserIdAndRoleId(userId, roleId);
        verify(adminAuditService).log(eq(AdminAuditAction.ROLE_REMOVED), eq("admin"), any(), any());
    }
}

