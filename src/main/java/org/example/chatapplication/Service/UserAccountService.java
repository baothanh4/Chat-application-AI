package org.example.chatapplication.Service;

import lombok.RequiredArgsConstructor;
import org.example.chatapplication.DTO.Request.CreateUserRequest;
import org.example.chatapplication.DTO.Response.UserResponse;
import org.example.chatapplication.Model.Entity.UserAccount;
import org.example.chatapplication.Repository.UserAccountRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserAccountService {
    private final UserAccountRepository userAccountRepository;
    private final FileStorageService fileStorageService;
    private final PresenceService presenceService;
    private final PasswordHasher passwordHasher;

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

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
