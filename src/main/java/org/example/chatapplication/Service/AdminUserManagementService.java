package org.example.chatapplication.Service;

import lombok.RequiredArgsConstructor;
import org.example.chatapplication.DTO.Request.AdminCreateUserRequest;
import org.example.chatapplication.DTO.Request.AdminUpdateUserRequest;
import org.example.chatapplication.DTO.Response.AdminUserDetailResponse;
import org.example.chatapplication.DTO.Response.AdminUserSummaryResponse;
import org.example.chatapplication.DTO.Response.AdminUserStatusResponse;
import org.example.chatapplication.Model.Entity.UserAccount;
import org.example.chatapplication.Model.Enum.AdminAuditAction;
import org.example.chatapplication.Model.Enum.UserRole;
import org.example.chatapplication.Repository.UserAccountRepository;
import org.springframework.dao.DataIntegrityViolationException;
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
    private final PasswordHasher passwordHasher;

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

    @Transactional(readOnly = true)
    public AdminUserDetailResponse getUser(UUID userId) {
        return toDetailResponse(requireUser(userId));
    }

    @Transactional
    public AdminUserDetailResponse createUser(AdminCreateUserRequest request, String actorUsername) {
        String username = request.getUsername().trim();
        if (userAccountRepository.findByUsernameIgnoreCase(username).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Username already exists");
        }

        UserAccount user = new UserAccount();
        user.setUsername(username);
        user.setDisplayName(request.getDisplayName().trim());
        user.setFullName(trimToNull(request.getFullName()));
        user.setPhoneNumber(trimToNull(request.getPhoneNumber()));
        user.setAddress(trimToNull(request.getAddress()));
        user.setBio(trimToNull(request.getBio()));
        user.setDateOfBirth(request.getDateOfBirth());
        user.setGender(trimToNull(request.getGender()));
        user.setEmail(trimToNull(request.getEmail()));
        user.setAvatarPath(trimToNull(request.getAvatarPath()));
        user.setPasswordHash(passwordHasher.hash(request.getPassword().trim()));
        user.setRole(request.getRole() == null ? UserRole.USER : request.getRole());
        user.setAccountLocked(Boolean.TRUE.equals(request.getAccountLocked()));

        UserAccount saved = saveUser(user);
        adminAuditService.log(AdminAuditAction.USER_CREATED, actorUsername, saved.getId(), "Created account: " + saved.getUsername());
        return toDetailResponse(saved);
    }

    @Transactional
    public AdminUserDetailResponse updateUser(UUID userId, AdminUpdateUserRequest request, String actorUsername) {
        UserAccount user = requireUser(userId);
        String username = request.getUsername().trim();

        userAccountRepository.findByUsernameIgnoreCase(username)
                .filter(existing -> !existing.getId().equals(userId))
                .ifPresent(existing -> {
                    throw new ResponseStatusException(HttpStatus.CONFLICT, "Username already exists");
                });

        if (user.getUsername().equalsIgnoreCase(actorUsername)) {
            if (Boolean.TRUE.equals(request.getAccountLocked())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "You cannot lock your own account");
            }
            if (request.getRole() != null && request.getRole() != user.getRole()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "You cannot change your own role");
            }
        }

        user.setUsername(username);
        user.setDisplayName(request.getDisplayName().trim());
        user.setFullName(trimToNull(request.getFullName()));
        user.setPhoneNumber(trimToNull(request.getPhoneNumber()));
        user.setAddress(trimToNull(request.getAddress()));
        user.setBio(trimToNull(request.getBio()));
        user.setDateOfBirth(request.getDateOfBirth());
        user.setGender(trimToNull(request.getGender()));
        user.setEmail(trimToNull(request.getEmail()));
        user.setAvatarPath(trimToNull(request.getAvatarPath()));

        if (request.getPassword() != null && !request.getPassword().isBlank()) {
            user.setPasswordHash(passwordHasher.hash(request.getPassword().trim()));
        }
        if (request.getRole() != null) {
            user.setRole(request.getRole());
        }
        if (request.getAccountLocked() != null) {
            user.setAccountLocked(request.getAccountLocked());
        }

        UserAccount saved = saveUser(user);
        adminAuditService.log(AdminAuditAction.USER_UPDATED, actorUsername, saved.getId(), "Updated account: " + saved.getUsername());
        return toDetailResponse(saved);
    }

    @Transactional
    public AdminUserStatusResponse deleteUser(UUID userId, String actorUsername) {
        UserAccount user = requireUser(userId);
        if (user.getUsername().equalsIgnoreCase(actorUsername)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "You cannot delete your own account");
        }

        String username = user.getUsername();
        try {
            userAccountRepository.delete(user);
            userAccountRepository.flush();
        } catch (DataIntegrityViolationException ex) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Cannot delete user due to existing related data", ex);
        }

        adminAuditService.log(AdminAuditAction.USER_DELETED, actorUsername, userId, "Deleted account: " + username);
        return new AdminUserStatusResponse(userId, username, user.getRole(), user.isAccountLocked(), "User has been deleted");
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

    private AdminUserDetailResponse toDetailResponse(UserAccount user) {
        return new AdminUserDetailResponse(
                user.getId(),
                user.getUsername(),
                user.getDisplayName(),
                user.getFullName(),
                user.getPhoneNumber(),
                user.getAddress(),
                user.getBio(),
                user.getDateOfBirth(),
                user.getGender(),
                user.getEmail(),
                user.getAvatarPath(),
                user.getRole(),
                user.isAccountLocked(),
                user.getLastSeenAt(),
                user.getCreatedAt(),
                user.getUpdatedAt()
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

    private UserAccount saveUser(UserAccount user) {
        try {
            return userAccountRepository.save(user);
        } catch (DataIntegrityViolationException ex) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Username already exists", ex);
        }
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}

