package org.example.chatapplication.Service;

import lombok.RequiredArgsConstructor;
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
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RoleManagementService {

    private final RoleRepository roleRepository;
    private final UserAccountRepository userAccountRepository;
    private final UserRoleAssignmentRepository userRoleAssignmentRepository;
    private final AdminAuditService adminAuditService;

    @Transactional(readOnly = true)
    public List<RoleResponse> listAllRoles() {
        return roleRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public RoleResponse getRole(UUID roleId) {
        Role role = requireRole(roleId);
        return toResponse(role);
    }

    @Transactional
    public RoleResponse createRole(CreateRoleRequest request, String actorUsername) {
        String name = request.getName().trim();
        if (roleRepository.findByNameIgnoreCase(name).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Role name already exists: " + name);
        }

        Role role = new Role();
        role.setName(name);
        role.setDescription(trimToNull(request.getDescription()));
        role.setBuiltIn(false);

        Role saved = roleRepository.save(role);
        adminAuditService.log(AdminAuditAction.ROLE_CREATED, actorUsername, saved.getId(),
                "Created role: " + saved.getName());
        return toResponse(saved);
    }

    @Transactional
    public RoleResponse updateRole(UUID roleId, CreateRoleRequest request, String actorUsername) {
        Role role = requireRole(roleId);

        if (role.isBuiltIn()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot modify built-in role: " + role.getName());
        }

        String newName = request.getName().trim();
        roleRepository.findByNameIgnoreCase(newName)
                .filter(existing -> !existing.getId().equals(roleId))
                .ifPresent(existing -> {
                    throw new ResponseStatusException(HttpStatus.CONFLICT, "Role name already exists: " + newName);
                });

        role.setName(newName);
        role.setDescription(trimToNull(request.getDescription()));

        Role saved = roleRepository.save(role);
        adminAuditService.log(AdminAuditAction.ROLE_UPDATED, actorUsername, saved.getId(),
                "Updated role: " + saved.getName());
        return toResponse(saved);
    }

    @Transactional
    public void deleteRole(UUID roleId, String actorUsername) {
        Role role = requireRole(roleId);

        if (role.isBuiltIn()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot delete built-in role: " + role.getName());
        }

        String roleName = role.getName();
        userRoleAssignmentRepository.deleteAll(
                userRoleAssignmentRepository.findAll().stream()
                        .filter(a -> a.getRole().getId().equals(roleId))
                        .toList()
        );
        roleRepository.delete(role);
        adminAuditService.log(AdminAuditAction.ROLE_DELETED, actorUsername, roleId,
                "Deleted role: " + roleName);
    }

    @Transactional
    public UserRolesResponse assignRoleToUser(UUID userId, AssignRoleRequest request, String actorUsername) {
        UserAccount user = requireUser(userId);
        Role role = requireRole(request.getRoleId());

        boolean alreadyAssigned = userRoleAssignmentRepository.findByUserId(userId).stream()
                .anyMatch(a -> a.getRole().getId().equals(role.getId()));

        if (alreadyAssigned) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "User already has this role assigned");
        }

        UserRoleAssignment assignment = new UserRoleAssignment(userId, role);
        userRoleAssignmentRepository.save(assignment);

        adminAuditService.log(AdminAuditAction.ROLE_ASSIGNED, actorUsername, user.getId(),
                "Assigned role " + role.getName() + " to user " + user.getUsername());

        return getUserRoles(userId);
    }

    @Transactional
    public void removeRoleFromUser(UUID userId, UUID roleId, String actorUsername) {
        UserAccount user = requireUser(userId);
        Role role = requireRole(roleId);

        userRoleAssignmentRepository.deleteByUserIdAndRoleId(userId, roleId);

        adminAuditService.log(AdminAuditAction.ROLE_REMOVED, actorUsername, user.getId(),
                "Removed role " + role.getName() + " from user " + user.getUsername());
    }

    @Transactional(readOnly = true)
    public UserRolesResponse getUserRoles(UUID userId) {
        UserAccount user = requireUser(userId);
        List<RoleResponse> roles = userRoleAssignmentRepository.findByUserId(userId).stream()
                .map(a -> toResponse(a.getRole()))
                .toList();
        return new UserRolesResponse(userId, roles);
    }

    private Role requireRole(UUID roleId) {
        return roleRepository.findById(roleId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Role not found: " + roleId));
    }

    private UserAccount requireUser(UUID userId) {
        return userAccountRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found: " + userId));
    }

    private RoleResponse toResponse(Role role) {
        return new RoleResponse(
                role.getId(),
                role.getName(),
                role.getDescription(),
                role.isBuiltIn(),
                role.getCreatedAt(),
                role.getUpdatedAt()
        );
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}

