package org.example.chatapplication.Controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.chatapplication.DTO.Request.UpdateUserRoleRequest;
import org.example.chatapplication.DTO.Response.AdminUserSummaryResponse;
import org.example.chatapplication.DTO.Response.AdminUserStatusResponse;
import org.example.chatapplication.Service.AdminUserManagementService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminUserController {

    private final AdminUserManagementService adminUserManagementService;

    @GetMapping
    ResponseEntity<List<AdminUserSummaryResponse>> listUsers(@RequestParam(required = false) String query,
                                                             @RequestParam(defaultValue = "50") int limit) {
        return ResponseEntity.ok(adminUserManagementService.listUsers(query, limit));
    }

    @PatchMapping("/{userId}/lock")
    ResponseEntity<AdminUserStatusResponse> lockUser(@PathVariable UUID userId, Authentication authentication) {
        return ResponseEntity.ok(adminUserManagementService.lockUser(userId, authentication.getName()));
    }

    @PatchMapping("/{userId}/unlock")
    ResponseEntity<AdminUserStatusResponse> unlockUser(@PathVariable UUID userId, Authentication authentication) {
        return ResponseEntity.ok(adminUserManagementService.unlockUser(userId, authentication.getName()));
    }

    @PatchMapping("/{userId}/role")
    ResponseEntity<AdminUserStatusResponse> updateUserRole(@PathVariable UUID userId,
                                                           @RequestBody @Valid UpdateUserRoleRequest request,
                                                           Authentication authentication) {
        return ResponseEntity.ok(adminUserManagementService.updateUserRole(userId, request.getRole(), authentication.getName()));
    }
}

