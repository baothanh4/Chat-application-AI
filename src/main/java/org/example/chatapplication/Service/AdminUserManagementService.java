package org.example.chatapplication.Service;

import lombok.RequiredArgsConstructor;
import org.example.chatapplication.DTO.Response.AdminUserSummaryResponse;
import org.example.chatapplication.DTO.Response.AdminUserStatusResponse;
import org.example.chatapplication.Model.Entity.UserAccount;
import org.example.chatapplication.Model.Enum.AdminAuditAction;
import org.example.chatapplication.Model.Enum.UserRole;
import org.example.chatapplication.Repository.UserAccountRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AdminUserManagementService {

    private final UserAccountRepository userAccountRepository;
    private final AdminAuditService adminAuditService;

    @Transactional(readOnly = true)
    public List<AdminUserSummaryResponse> listUsers(String query, int limit) {
        int normalizedLimit = Math.max(1, Math.min(limit, 200));
        PageRequest pageRequest = PageRequest.of(0, normalizedLimit, Sort.by(Sort.Direction.DESC, "createdAt"));

        List<UserAccount> users;
        if (query == null || query.isBlank()) {
            users = userAccountRepository.findAll(pageRequest).getContent();
        } else {
            String normalizedQuery = query.trim();
            users = userAccountRepository.findByUsernameContainingIgnoreCaseOrDisplayNameContainingIgnoreCase(
                    normalizedQuery,
                    normalizedQuery,
                    pageRequest
            );
        }

        return users.stream()
                .map(this::toSummaryResponse)
                .toList();
    }

    @Transactional
    public AdminUserStatusResponse lockUser(UUID userId, String actorUsername) {
        UserAccount user = requireUser(userId);
        if (user.getUsername().equalsIgnoreCase(actorUsername)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "You cannot lock your own account");
        }

        user.setAccountLocked(true);
        UserAccount saved = userAccountRepository.save(user);
        adminAuditService.log(AdminAuditAction.USER_LOCKED, actorUsername, saved.getId(), "Locked account: " + saved.getUsername());
        return toResponse(saved, "User account has been locked");
    }

    @Transactional
    public AdminUserStatusResponse unlockUser(UUID userId, String actorUsername) {
        UserAccount user = requireUser(userId);
        user.setAccountLocked(false);
        UserAccount saved = userAccountRepository.save(user);
        adminAuditService.log(AdminAuditAction.USER_UNLOCKED, actorUsername, saved.getId(), "Unlocked account: " + saved.getUsername());
        return toResponse(saved, "User account has been unlocked");
    }

    @Transactional
    public AdminUserStatusResponse updateUserRole(UUID userId, UserRole role, String actorUsername) {
        if (role == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Role is required");
        }

        UserAccount user = requireUser(userId);
        if (user.getUsername().equalsIgnoreCase(actorUsername)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "You cannot change your own role");
        }

        UserRole previousRole = user.getRole();
        user.setRole(role);
        UserAccount saved = userAccountRepository.save(user);
        adminAuditService.log(
                AdminAuditAction.USER_ROLE_CHANGED,
                actorUsername,
                saved.getId(),
                "Changed role for " + saved.getUsername() + " from " + previousRole + " to " + role
        );
        return toResponse(saved, "User role has been updated");
    }

    private UserAccount requireUser(UUID userId) {
        return userAccountRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found: " + userId));
    }

    private AdminUserStatusResponse toResponse(UserAccount user, String message) {
        return new AdminUserStatusResponse(
                user.getId(),
                user.getUsername(),
                user.getRole(),
                user.isAccountLocked(),
                message
        );
    }

    private AdminUserSummaryResponse toSummaryResponse(UserAccount user) {
        return new AdminUserSummaryResponse(
                user.getId(),
                user.getUsername(),
                user.getDisplayName(),
                user.getRole(),
                user.isAccountLocked(),
                user.getLastSeenAt(),
                user.getCreatedAt()
        );
    }
}

