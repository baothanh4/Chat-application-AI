package org.example.chatapplication.Controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.chatapplication.DTO.Request.AdminCreateUserRequest;
import org.example.chatapplication.DTO.Request.AdminUpdateUserRequest;
import org.example.chatapplication.DTO.Request.UpdateUserRoleRequest;
import org.example.chatapplication.DTO.Response.AdminUserDetailResponse;
import org.example.chatapplication.DTO.Response.AdminUserSummaryResponse;
import org.example.chatapplication.DTO.Response.AdminUserStatusResponse;
import org.example.chatapplication.Service.AdminUserManagementService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
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

    @GetMapping("/{userId}")
    ResponseEntity<AdminUserDetailResponse> getUser(@PathVariable UUID userId) {
        return ResponseEntity.ok(adminUserManagementService.getUser(userId));
    }

    @PostMapping
    ResponseEntity<AdminUserDetailResponse> createUser(@RequestBody @Valid AdminCreateUserRequest request,
                                                       Authentication authentication) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(adminUserManagementService.createUser(request, authentication.getName()));
    }

    @PutMapping("/{userId}")
    ResponseEntity<AdminUserDetailResponse> updateUser(@PathVariable UUID userId,
                                                       @RequestBody @Valid AdminUpdateUserRequest request,
                                                       Authentication authentication) {
        return ResponseEntity.ok(adminUserManagementService.updateUser(userId, request, authentication.getName()));
    }

    @DeleteMapping("/{userId}")
    ResponseEntity<AdminUserStatusResponse> deleteUser(@PathVariable UUID userId, Authentication authentication) {
        return ResponseEntity.ok(adminUserManagementService.deleteUser(userId, authentication.getName()));
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

