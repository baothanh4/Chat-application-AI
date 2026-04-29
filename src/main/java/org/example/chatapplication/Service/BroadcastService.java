package org.example.chatapplication.Service;

import lombok.RequiredArgsConstructor;
import org.example.chatapplication.DTO.Request.CreateBroadcastRequest;
import org.example.chatapplication.DTO.Response.BroadcastResponse;
import org.example.chatapplication.Model.Entity.Broadcast;
import org.example.chatapplication.Model.Entity.UserAccount;
import org.example.chatapplication.Model.Enum.AdminAuditAction;
import org.example.chatapplication.Repository.BroadcastRepository;
import org.example.chatapplication.Repository.UserAccountRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class BroadcastService {

    private final BroadcastRepository broadcastRepository;
    private final UserAccountRepository userAccountRepository;
    private final AdminAuditService adminAuditService;
    private final NotificationService notificationService;

    @Transactional
    public BroadcastResponse createBroadcast(CreateBroadcastRequest request, String actorUsername) {
        UserAccount creator = userAccountRepository.findByUsernameIgnoreCase(actorUsername)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found: " + actorUsername));

        Broadcast broadcast = new Broadcast();
        broadcast.setTitle(request.getTitle().trim());
        broadcast.setMessage(request.getMessage().trim());
        broadcast.setLinkUrl(trimToNull(request.getLinkUrl()));
        broadcast.setCreatedByUser(creator);

        Broadcast saved = broadcastRepository.save(broadcast);
        adminAuditService.log(
                AdminAuditAction.BROADCAST_CREATED,
                actorUsername,
                saved.getId(),
                "Created broadcast: " + saved.getTitle()
        );

        // Send to all active users (or queue for offline users)
        sendBroadcastToAllUsers(saved);

        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<BroadcastResponse> listBroadcasts() {
        return broadcastRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public BroadcastResponse getBroadcast(UUID broadcastId) {
        Broadcast broadcast = broadcastRepository.findById(broadcastId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Broadcast not found: " + broadcastId));
        return toResponse(broadcast);
    }

    @Transactional
    public void deleteBroadcast(UUID broadcastId, String actorUsername) {
        Broadcast broadcast = broadcastRepository.findById(broadcastId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Broadcast not found: " + broadcastId));

        String title = broadcast.getTitle();
        broadcastRepository.delete(broadcast);
        adminAuditService.log(
                AdminAuditAction.BROADCAST_DELETED,
                actorUsername,
                broadcastId,
                "Deleted broadcast: " + title
        );
    }

    private void sendBroadcastToAllUsers(Broadcast broadcast) {
        // Get all active/online users and send notification
        // For now, we use NotificationService to queue or log notification
        try {
            // In a real system, this would queue notifications via push service
            // For MVP, we can log or use in-memory notification queue
            notificationService.sendBroadcastNotification(broadcast);
        } catch (Exception ex) {
            // Log error but don't fail broadcast creation
        }
    }

    private BroadcastResponse toResponse(Broadcast broadcast) {
        return new BroadcastResponse(
                broadcast.getId(),
                broadcast.getTitle(),
                broadcast.getMessage(),
                broadcast.getLinkUrl(),
                broadcast.getCreatedByUser().getId(),
                broadcast.getCreatedByUser().getUsername(),
                broadcast.getCreatedAt()
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

