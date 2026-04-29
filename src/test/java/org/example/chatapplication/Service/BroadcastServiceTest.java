package org.example.chatapplication.Service;

import org.example.chatapplication.DTO.Request.CreateBroadcastRequest;
import org.example.chatapplication.DTO.Response.BroadcastResponse;
import org.example.chatapplication.Model.Entity.Broadcast;
import org.example.chatapplication.Model.Entity.UserAccount;
import org.example.chatapplication.Model.Enum.AdminAuditAction;
import org.example.chatapplication.Repository.BroadcastRepository;
import org.example.chatapplication.Repository.UserAccountRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
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
class BroadcastServiceTest {

    @Mock
    private BroadcastRepository broadcastRepository;

    @Mock
    private UserAccountRepository userAccountRepository;

    @Mock
    private AdminAuditService adminAuditService;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private BroadcastService broadcastService;

    @Test
    void createBroadcastShouldCreateAndReturnBroadcast() {
        UUID userId = UUID.randomUUID();
        CreateBroadcastRequest request = new CreateBroadcastRequest("Maintenance", "Server maintenance at 2am", "https://example.com");

        UserAccount creator = mock(UserAccount.class);
        creator.setId(userId);
        creator.setUsername("admin");

        Broadcast broadcast = new Broadcast();
        broadcast.setId(UUID.randomUUID());
        broadcast.setTitle("Maintenance");
        broadcast.setMessage("Server maintenance at 2am");
        broadcast.setLinkUrl("https://example.com");
        broadcast.setCreatedByUser(creator);

        when(userAccountRepository.findByUsernameIgnoreCase("admin")).thenReturn(Optional.of(creator));
        when(broadcastRepository.save(any(Broadcast.class))).thenReturn(broadcast);

        BroadcastResponse response = broadcastService.createBroadcast(request, "admin");

        assertThat(response.getTitle()).isEqualTo("Maintenance");
        assertThat(response.getMessage()).isEqualTo("Server maintenance at 2am");
        verify(adminAuditService).log(eq(AdminAuditAction.BROADCAST_CREATED), eq("admin"), any(), any());
        verify(notificationService).sendBroadcastNotification(broadcast);
    }

    @Test
    void createBroadcastShouldFailIfUserNotFound() {
        CreateBroadcastRequest request = new CreateBroadcastRequest("Test", "Test message", null);

        when(userAccountRepository.findByUsernameIgnoreCase("admin")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> broadcastService.createBroadcast(request, "admin"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("User not found");
    }

    @Test
    void listBroadcastsShouldReturnBroadcastsOrderedByCreatedAtDesc() {
        UUID creatorId = UUID.randomUUID();
        UserAccount creator = mock(UserAccount.class);
        creator.setId(creatorId);
        creator.setUsername("admin");

        Broadcast broadcast1 = new Broadcast();
        broadcast1.setId(UUID.randomUUID());
        broadcast1.setTitle("Broadcast 1");
        broadcast1.setCreatedByUser(creator);

        Broadcast broadcast2 = new Broadcast();
        broadcast2.setId(UUID.randomUUID());
        broadcast2.setTitle("Broadcast 2");
        broadcast2.setCreatedByUser(creator);

        when(broadcastRepository.findAllByOrderByCreatedAtDesc()).thenReturn(List.of(broadcast2, broadcast1));

        List<BroadcastResponse> response = broadcastService.listBroadcasts();

        assertThat(response).hasSize(2);
        assertThat(response.get(0).getTitle()).isEqualTo("Broadcast 2");
        assertThat(response.get(1).getTitle()).isEqualTo("Broadcast 1");
    }

    @Test
    void getBroadcastShouldReturnBroadcast() {
        UUID broadcastId = UUID.randomUUID();
        UUID creatorId = UUID.randomUUID();

        UserAccount creator = mock(UserAccount.class);
        creator.setId(creatorId);
        creator.setUsername("admin");

        Broadcast broadcast = new Broadcast();
        broadcast.setId(broadcastId);
        broadcast.setTitle("Test Broadcast");
        broadcast.setMessage("This is a test");
        broadcast.setCreatedByUser(creator);

        when(broadcastRepository.findById(broadcastId)).thenReturn(Optional.of(broadcast));

        BroadcastResponse response = broadcastService.getBroadcast(broadcastId);

        assertThat(response.getId()).isEqualTo(broadcastId);
        assertThat(response.getTitle()).isEqualTo("Test Broadcast");
    }

    @Test
    void getBroadcastShouldFailIfNotFound() {
        UUID broadcastId = UUID.randomUUID();

        when(broadcastRepository.findById(broadcastId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> broadcastService.getBroadcast(broadcastId))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Broadcast not found");
    }

    @Test
    void deleteBroadcastShouldDeleteAndAudit() {
        UUID broadcastId = UUID.randomUUID();
        UUID creatorId = UUID.randomUUID();

        UserAccount creator = mock(UserAccount.class);
        creator.setId(creatorId);
        creator.setUsername("admin");

        Broadcast broadcast = new Broadcast();
        broadcast.setId(broadcastId);
        broadcast.setTitle("Test Broadcast");
        broadcast.setCreatedByUser(creator);

        when(broadcastRepository.findById(broadcastId)).thenReturn(Optional.of(broadcast));

        broadcastService.deleteBroadcast(broadcastId, "admin");

        verify(broadcastRepository).delete(broadcast);
        verify(adminAuditService).log(eq(AdminAuditAction.BROADCAST_DELETED), eq("admin"), eq(broadcastId), any());
    }
}

