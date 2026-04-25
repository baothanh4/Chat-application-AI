package org.example.chatapplication.Service;

import lombok.RequiredArgsConstructor;
import org.example.chatapplication.DTO.Request.CreateUserRequest;
import org.example.chatapplication.DTO.Request.ChangePasswordRequest;
import org.example.chatapplication.DTO.Response.UserResponse;
import org.example.chatapplication.Model.Entity.UserAccount;
import org.example.chatapplication.Repository.UserAccountRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserAccountService {
    private final UserAccountRepository userAccountRepository;
    private final FileStorageService fileStorageService;
    private final PresenceService presenceService;
    private final PasswordHasher passwordHasher;
    private final FaceVerificationService faceVerificationService;

    @Transactional
    public UserAccount createOrUpdate(CreateUserRequest request) {
        UserAccount user = userAccountRepository.findByUsernameIgnoreCase(request.getUsername()).orElseGet(UserAccount::new);
        user.setUsername(request.getUsername().trim());
        user.setDisplayName(request.getDisplayName().trim());
        user.setFullName(trimToNull(request.getFullName()));
        user.setPhoneNumber(trimToNull(request.getPhoneNumber()));
        user.setAddress(trimToNull(request.getAddress()));
        user.setBio(trimToNull(request.getBio()));
        user.setDateOfBirth(request.getDateOfBirth());
        user.setGender(trimToNull(request.getGender()));
        user.setEmail(request.getEmail());
        user.setAvatarPath(trimToNull(request.getAvatarPath()));
        
        if (request.getPassword() != null && !request.getPassword().trim().isEmpty()) {
            user.setPasswordHash(passwordHasher.hash(request.getPassword().trim()));
        }

        try {
            return userAccountRepository.save(user);
        } catch (DataIntegrityViolationException ex) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Username already exists", ex);
        }
    }

    @Transactional
    public UserAccount updateProfile(UUID userId, org.example.chatapplication.DTO.Request.UpdateUserRequest request) {
        UserAccount user = requireUser(userId);
        user.setDisplayName(request.getDisplayName().trim());
        user.setFullName(trimToNull(request.getFullName()));
        user.setPhoneNumber(trimToNull(request.getPhoneNumber()));
        user.setAddress(trimToNull(request.getAddress()));
        user.setBio(trimToNull(request.getBio()));
        user.setDateOfBirth(request.getDateOfBirth());
        user.setGender(trimToNull(request.getGender()));
        user.setEmail(request.getEmail());
        
        return userAccountRepository.save(user);
    }

    @Transactional
    public UserAccount changePassword(UUID userId, ChangePasswordRequest request) {
        UserAccount user = requireUser(userId);
        String oldPassword = trimToNull(request.getOldPassword());
        String newPassword = trimToNull(request.getNewPassword());
        String confirmNewPassword = trimToNull(request.getConfirmNewPassword());

        if (oldPassword == null || newPassword == null || confirmNewPassword == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Old password, new password and confirmation are required");
        }

        if (user.getPasswordHash() == null || !passwordHasher.matches(oldPassword, user.getPasswordHash())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Old password is incorrect");
        }

        if (!newPassword.equals(confirmNewPassword)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "New password confirmation does not match");
        }

        if (newPassword.equals(oldPassword)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "New password must be different from old password");
        }

        user.setPasswordHash(passwordHasher.hash(newPassword));
        return userAccountRepository.save(user);
    }

    @Transactional
    public UserAccount disableFaceLogin(UUID userId) {
        UserAccount user = requireUser(userId);
        user.setFaceLoginEnabled(Boolean.FALSE);
        return userAccountRepository.save(user);
    }

    @Transactional
    public UserAccount enableFaceLogin(UUID userId) {
        UserAccount user = requireUser(userId);
        if (!hasFaceEnrollment(user)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No face template exists to enable FaceID");
        }

        user.setFaceLoginEnabled(Boolean.TRUE);
        if (user.getFaceEnrolledAt() == null) {
            user.setFaceEnrolledAt(Instant.now());
        }
        return userAccountRepository.save(user);
    }

    @Transactional
    public UserAccount deleteFaceEnrollment(UUID userId) {
        UserAccount user = requireUser(userId);
        fileStorageService.deleteStoredFile(user.getFaceTemplatePath());
        clearFaceEnrollment(user);
        return userAccountRepository.save(user);
    }

    @Transactional
    public UserAccount replaceFaceEnrollment(UUID userId, MultipartFile faceImage) {
        UserAccount user = requireUser(userId);
        applyFaceEnrollment(user, faceImage, true);
        return userAccountRepository.save(user);
    }

    public UserAccount applyFaceEnrollment(UserAccount user, MultipartFile faceImage, boolean enableFaceLogin) {
        if (user == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found");
        }

        String faceSignature = faceVerificationService.generateSignature(faceImage);
        ensureFaceIsUnique(faceSignature, user.getId());

        String previousFacePath = trimToNull(user.getFaceTemplatePath());
        String faceTemplatePath = fileStorageService.storeFaceTemplate(faceImage);

        if (previousFacePath != null) {
            fileStorageService.deleteStoredFile(previousFacePath);
        }

        user.setFaceTemplatePath(faceTemplatePath);
        user.setFaceTemplateHash(faceSignature);
        user.setFaceLoginEnabled(enableFaceLogin);
        user.setFaceEnrolledAt(Instant.now());
        return user;
    }

    @Transactional(readOnly = true)
    public UserAccount authenticate(String username, String password) {
        UserAccount user = userAccountRepository.findByUsernameIgnoreCase(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid username or password"));
        
        if (user.getPasswordHash() == null || !passwordHasher.matches(password, user.getPasswordHash())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid username or password");
        }
        return user;
    }

    @Transactional(readOnly = true)
    public UserAccount requireUser(UUID userId) {
        return userAccountRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found: " + userId));
    }

    @Transactional
    public UserAccount uploadAvatar(UUID userId, org.springframework.web.multipart.MultipartFile file) {
        UserAccount user = requireUser(userId);
        String avatarPath = fileStorageService.storeAvatar(file);
        user.setAvatarPath(avatarPath);
        return userAccountRepository.save(user);
    }

    @Transactional
    public UserAccount deleteAvatar(UUID userId) {
        UserAccount user = requireUser(userId);
        if (user.getAvatarPath() != null) {
            fileStorageService.deleteStoredFile(user.getAvatarPath());
            user.setAvatarPath(null);
        }
        return userAccountRepository.save(user);
    }

    @Transactional(readOnly = true)
    public UserResponse toResponse(UserAccount user) {
        return new UserResponse(
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
                user.getFaceTemplatePath(),
                user.isFaceLoginEnabled(),
                user.getFaceEnrolledAt(),
                presenceService.getPresence(user.getId())
        );
    }

    private void ensureFaceIsUnique(String candidateSignature, UUID excludedUserId) {
        userAccountRepository.findByFaceLoginEnabledTrueAndFaceTemplateHashIsNotNull().stream()
                .filter(existing -> excludedUserId == null || !excludedUserId.equals(existing.getId()))
                .filter(existing -> faceVerificationService.distance(resolveStoredFaceSignature(existing), candidateSignature) <= 140)
                .findFirst()
                .ifPresent(conflict -> {
                    throw new ResponseStatusException(HttpStatus.CONFLICT, "This face is already enrolled for another account");
                });
    }

    private String resolveStoredFaceSignature(UserAccount user) {
        if (user == null) {
            return null;
        }

        if (user.getFaceTemplatePath() != null && !user.getFaceTemplatePath().isBlank()) {
            try {
                return faceVerificationService.generateSignatureFromStoredPath(user.getFaceTemplatePath());
            } catch (RuntimeException ex) {
                if (user.getFaceTemplateHash() != null && !user.getFaceTemplateHash().isBlank()) {
                    return user.getFaceTemplateHash();
                }
            }
        }

        return user.getFaceTemplateHash();
    }

    private void clearFaceEnrollment(UserAccount user) {
        user.setFaceTemplatePath(null);
        user.setFaceTemplateHash(null);
        user.setFaceEnrolledAt(null);
        user.setFaceLoginEnabled(Boolean.FALSE);
    }

    private boolean hasFaceEnrollment(UserAccount user) {
        return user != null && ((user.getFaceTemplatePath() != null && !user.getFaceTemplatePath().isBlank())
                || (user.getFaceTemplateHash() != null && !user.getFaceTemplateHash().isBlank()));
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
