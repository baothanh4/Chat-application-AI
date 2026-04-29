package org.example.chatapplication.Service;

import org.example.chatapplication.DTO.Response.ImpersonationTokenResponse;
import org.example.chatapplication.Model.Entity.ImpersonationToken;
import org.example.chatapplication.Model.Entity.UserAccount;
import org.example.chatapplication.Model.Enum.AdminAuditAction;
import org.example.chatapplication.Repository.ImpersonationTokenRepository;
import org.example.chatapplication.Repository.UserAccountRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
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
class ImpersonationServiceTest {

    @Mock
    private ImpersonationTokenRepository impersonationTokenRepository;

    @Mock
    private UserAccountRepository userAccountRepository;

    @Mock
    private AdminAuditService adminAuditService;

    @InjectMocks
    private ImpersonationService impersonationService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(impersonationService, "expirationMinutes", 15);
    }

    @Test
    void createImpersonationTokenShouldCreateAndReturnToken() {
        UUID adminId = UUID.randomUUID();
        UUID targetId = UUID.randomUUID();

        UserAccount admin = new UserAccount();
        admin.setId(adminId);
        admin.setUsername("admin");

        UserAccount target = new UserAccount();
        target.setId(targetId);
        target.setUsername("user");

        ImpersonationToken token = new ImpersonationToken();
        token.setId(UUID.randomUUID());
        token.setAdminUser(admin);
        token.setTargetUser(target);
        token.setToken("test-token");
        token.setExpiresAt(Instant.now().plusSeconds(900)); // 15 minutes

        when(userAccountRepository.findByUsernameIgnoreCase("admin")).thenReturn(Optional.of(admin));
        when(userAccountRepository.findById(targetId)).thenReturn(Optional.of(target));
        when(impersonationTokenRepository.save(any(ImpersonationToken.class))).thenReturn(token);

        ImpersonationTokenResponse response = impersonationService.createImpersonationToken(targetId, "admin", "support");

        assertThat(response.getTargetUsername()).isEqualTo("user");
        assertThat(response.getToken()).isNotNull();
        assertThat(response.getExpiresAt()).isNotNull();
        verify(adminAuditService).log(eq(AdminAuditAction.IMPERSONATION_CREATED), eq("admin"), eq(targetId), any());
    }

    @Test
    void createImpersonationTokenShouldFailIfAdminNotFound() {
        UUID targetId = UUID.randomUUID();

        when(userAccountRepository.findByUsernameIgnoreCase("admin")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> impersonationService.createImpersonationToken(targetId, "admin", "support"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Admin user not found");
    }

    @Test
    void createImpersonationTokenShouldFailIfTargetNotFound() {
        UUID adminId = UUID.randomUUID();
        UUID targetId = UUID.randomUUID();

        UserAccount admin = new UserAccount();
        admin.setId(adminId);

        when(userAccountRepository.findByUsernameIgnoreCase("admin")).thenReturn(Optional.of(admin));
        when(userAccountRepository.findById(targetId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> impersonationService.createImpersonationToken(targetId, "admin", "support"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Target user not found");
    }

    @Test
    void createImpersonationTokenShouldFailIfAdminImpersonatesSelf() {
        UUID adminId = UUID.randomUUID();

        UserAccount admin = new UserAccount();
        admin.setId(adminId);
        admin.setUsername("admin");

        when(userAccountRepository.findByUsernameIgnoreCase("admin")).thenReturn(Optional.of(admin));
        when(userAccountRepository.findById(adminId)).thenReturn(Optional.of(admin));

        assertThatThrownBy(() -> impersonationService.createImpersonationToken(adminId, "admin", "support"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("cannot impersonate themselves");
    }

    @Test
    void validateImpersonationTokenShouldReturnUserIfValid() {
        UUID targetId = UUID.randomUUID();

        UserAccount target = new UserAccount();
        target.setId(targetId);
        target.setUsername("user");

        ImpersonationToken token = new ImpersonationToken();
        token.setToken("test-token");
        token.setTargetUser(target);
        token.setExpiresAt(Instant.now().plusSeconds(600)); // expires in future
        token.setRevokedAt(null);

        when(impersonationTokenRepository.findByToken("test-token")).thenReturn(Optional.of(token));

        Optional<UserAccount> result = impersonationService.validateImpersonationToken("test-token");

        assertThat(result).isPresent();
        assertThat(result.get().getUsername()).isEqualTo("user");
    }

    @Test
    void validateImpersonationTokenShouldReturnEmptyIfExpired() {
        ImpersonationToken token = new ImpersonationToken();
        token.setToken("test-token");
        token.setExpiresAt(Instant.now().minusSeconds(600)); // expired
        token.setRevokedAt(null);

        when(impersonationTokenRepository.findByToken("test-token")).thenReturn(Optional.of(token));

        Optional<UserAccount> result = impersonationService.validateImpersonationToken("test-token");

        assertThat(result).isEmpty();
    }

    @Test
    void validateImpersonationTokenShouldReturnEmptyIfRevoked() {
        UUID targetId = UUID.randomUUID();

        UserAccount target = new UserAccount();
        target.setId(targetId);

        ImpersonationToken token = new ImpersonationToken();
        token.setToken("test-token");
        token.setTargetUser(target);
        token.setExpiresAt(Instant.now().plusSeconds(600));
        token.setRevokedAt(Instant.now().minusSeconds(300)); // revoked in past

        when(impersonationTokenRepository.findByToken("test-token")).thenReturn(Optional.of(token));

        Optional<UserAccount> result = impersonationService.validateImpersonationToken("test-token");

        assertThat(result).isEmpty();
    }

    @Test
    void revokeImpersonationTokenShouldRevokeAndAudit() {
        UUID adminId = UUID.randomUUID();
        UUID targetId = UUID.randomUUID();

        UserAccount admin = new UserAccount();
        admin.setId(adminId);

        UserAccount target = new UserAccount();
        target.setId(targetId);
        target.setUsername("user");

        ImpersonationToken token = new ImpersonationToken();
        token.setId(UUID.randomUUID());
        token.setAdminUser(admin);
        token.setTargetUser(target);
        token.setToken("test-token");

        when(impersonationTokenRepository.findByToken("test-token")).thenReturn(Optional.of(token));
        when(impersonationTokenRepository.save(any(ImpersonationToken.class))).thenAnswer(invocation -> invocation.getArgument(0));

        impersonationService.revokeImpersonationToken("test-token", "admin");

        assertThat(token.getRevokedAt()).isNotNull();
        verify(adminAuditService).log(eq(AdminAuditAction.IMPERSONATION_REVOKED), eq("admin"), eq(targetId), any());
    }
}

