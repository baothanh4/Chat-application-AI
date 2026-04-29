package org.example.chatapplication.Controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.chatapplication.DTO.Request.AssignRoleRequest;
import org.example.chatapplication.DTO.Request.CreateRoleRequest;
import org.example.chatapplication.DTO.Response.RoleResponse;
import org.example.chatapplication.DTO.Response.UserRolesResponse;
import org.example.chatapplication.Service.RoleManagementService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/roles")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class RoleManagementController {

    private final RoleManagementService roleManagementService;

    @GetMapping
    ResponseEntity<List<RoleResponse>> listAllRoles() {
        return ResponseEntity.ok(roleManagementService.listAllRoles());
    }

    @GetMapping("/{roleId}")
    ResponseEntity<RoleResponse> getRole(@PathVariable UUID roleId) {
        return ResponseEntity.ok(roleManagementService.getRole(roleId));
    }

    @PostMapping
    ResponseEntity<RoleResponse> createRole(@RequestBody @Valid CreateRoleRequest request,
                                           Authentication authentication) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(roleManagementService.createRole(request, authentication.getName()));
    }

    @PutMapping("/{roleId}")
    ResponseEntity<RoleResponse> updateRole(@PathVariable UUID roleId,
                                           @RequestBody @Valid CreateRoleRequest request,
                                           Authentication authentication) {
        return ResponseEntity.ok(roleManagementService.updateRole(roleId, request, authentication.getName()));
    }

    @DeleteMapping("/{roleId}")
    ResponseEntity<Void> deleteRole(@PathVariable UUID roleId, Authentication authentication) {
        roleManagementService.deleteRole(roleId, authentication.getName());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{roleId}/assign-to/{userId}")
    ResponseEntity<UserRolesResponse> assignRoleToUser(@PathVariable UUID roleId,
                                                      @PathVariable UUID userId,
                                                      Authentication authentication) {
        AssignRoleRequest request = new AssignRoleRequest(roleId);
        return ResponseEntity.ok(roleManagementService.assignRoleToUser(userId, request, authentication.getName()));
    }

    @DeleteMapping("/{roleId}/remove-from/{userId}")
    ResponseEntity<Void> removeRoleFromUser(@PathVariable UUID roleId,
                                           @PathVariable UUID userId,
                                           Authentication authentication) {
        roleManagementService.removeRoleFromUser(userId, roleId, authentication.getName());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/user/{userId}")
    ResponseEntity<UserRolesResponse> getUserRoles(@PathVariable UUID userId) {
        return ResponseEntity.ok(roleManagementService.getUserRoles(userId));
    }
}

