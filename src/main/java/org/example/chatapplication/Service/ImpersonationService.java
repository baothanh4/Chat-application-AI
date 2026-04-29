package org.example.chatapplication.Service;

import lombok.RequiredArgsConstructor;
import org.example.chatapplication.DTO.Response.ImpersonationTokenResponse;
import org.example.chatapplication.Model.Entity.ImpersonationToken;
import org.example.chatapplication.Model.Entity.UserAccount;
import org.example.chatapplication.Model.Enum.AdminAuditAction;
import org.example.chatapplication.Repository.ImpersonationTokenRepository;
import org.example.chatapplication.Repository.UserAccountRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ImpersonationService {

    private final ImpersonationTokenRepository impersonationTokenRepository;
    private final UserAccountRepository userAccountRepository;
    private final AdminAuditService adminAuditService;

    @Value("${impersonation.token.expiration-minutes:15}")
    private int expirationMinutes;

    private static final SecureRandom random = new SecureRandom();

    @Transactional
    public ImpersonationTokenResponse createImpersonationToken(UUID targetUserId, String adminUsername, String reason) {
        UserAccount admin = userAccountRepository.findByUsernameIgnoreCase(adminUsername)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Admin user not found"));

        UserAccount target = userAccountRepository.findById(targetUserId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Target user not found: " + targetUserId));

        if (admin.getId().equals(target.getId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Admin cannot impersonate themselves");
        }

        String token = generateToken();
        Instant expiresAt = Instant.now().plusSeconds(expirationMinutes * 60L);

        ImpersonationToken impersonation = new ImpersonationToken();
        impersonation.setAdminUser(admin);
        impersonation.setTargetUser(target);
        impersonation.setToken(token);
        impersonation.setExpiresAt(expiresAt);

        ImpersonationToken saved = impersonationTokenRepository.save(impersonation);

        adminAuditService.log(
                AdminAuditAction.IMPERSONATION_CREATED,
                adminUsername,
                targetUserId,
                "Created impersonation token for " + target.getUsername() + (reason != null ? " (reason: " + reason + ")" : "")
        );

        return new ImpersonationTokenResponse(
                token,
                expiresAt,
                target.getUsername(),
                "Impersonation token created. Can be used as JWT for " + expirationMinutes + " minutes."
        );
    }

    @Transactional(readOnly = true)
    public Optional<UserAccount> validateImpersonationToken(String token) {
        ImpersonationToken impToken = impersonationTokenRepository.findByToken(token)
                .orElse(null);

        if (impToken == null) {
            return Optional.empty();
        }

        // Check if token is expired
        if (Instant.now().isAfter(impToken.getExpiresAt())) {
            return Optional.empty();
        }

        // Check if token is revoked
        if (impToken.getRevokedAt() != null && Instant.now().isAfter(impToken.getRevokedAt())) {
            return Optional.empty();
        }

        return Optional.of(impToken.getTargetUser());
    }

    @Transactional
    public void revokeImpersonationToken(String token, String adminUsername) {
        ImpersonationToken impToken = impersonationTokenRepository.findByToken(token)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Impersonation token not found"));

        impToken.setRevokedAt(Instant.now());
        impersonationTokenRepository.save(impToken);

        adminAuditService.log(
                AdminAuditAction.IMPERSONATION_REVOKED,
                adminUsername,
                impToken.getTargetUser().getId(),
                "Revoked impersonation token for " + impToken.getTargetUser().getUsername()
        );
    }

    private String generateToken() {
        byte[] randomBytes = new byte[32];
        random.nextBytes(randomBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
    }
}

