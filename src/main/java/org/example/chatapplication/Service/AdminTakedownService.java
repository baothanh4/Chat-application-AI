package org.example.chatapplication.Service;

import lombok.RequiredArgsConstructor;
import org.example.chatapplication.DTO.Response.ChatMessageResponse;
import org.example.chatapplication.DTO.Response.UserResponse;
import org.example.chatapplication.Model.Entity.ChatMessage;
import org.example.chatapplication.Model.Enum.AdminAuditAction;
import org.example.chatapplication.Repository.ChatMessageRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AdminTakedownService {

    private final ChatMessageRepository chatMessageRepository;
    private final AdminAuditService adminAuditService;
    private final FileStorageService fileStorageService;
    private final ChatMessageService chatMessageService;

    @Transactional
    public ChatMessageResponse adminDeleteMessage(UUID messageId, String actorUsername, String reason) {
        ChatMessage message = chatMessageRepository.findById(messageId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Message not found: " + messageId));

        message.setDeletedForEveryone(true);
        message.setDeletedAt(Instant.now());
        message.setDeletedByUserId(null); // mark as admin deletion (null means admin action)
        ChatMessage saved = chatMessageRepository.save(message);

        adminAuditService.log(
                AdminAuditAction.MESSAGE_DELETED_BY_ADMIN,
                actorUsername,
                messageId,
                "Deleted message (reason: " + (reason != null ? reason : "moderation") + ")"
        );

        return toMessageResponse(saved);
    }

    @Transactional
    public void adminRemoveFileUpload(String filePath, String actorUsername, String reason) {
        try {
            fileStorageService.deleteStoredFile(filePath);
            adminAuditService.log(
                    AdminAuditAction.FILE_DELETED_BY_ADMIN,
                    actorUsername,
                    null,
                    "Deleted file: " + filePath + " (reason: " + (reason != null ? reason : "moderation") + ")"
            );
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to delete file", ex);
        }
    }

    private ChatMessageResponse toMessageResponse(ChatMessage message) {
        ChatMessageResponse response = new ChatMessageResponse();
        response.setId(message.getId());
        response.setConversationId(message.getConversation().getId());

        UserResponse userResponse = new UserResponse();
        userResponse.setId(message.getSender().getId());
        userResponse.setUsername(message.getSender().getUsername());
        userResponse.setDisplayName(message.getSender().getDisplayName());

        response.setSender(userResponse);
        response.setContent(message.getContent());
        response.setMessageType(message.getMessageType());
        response.setStatus(message.getStatus());
        response.setEdited(message.isEdited());
        response.setEditedAt(message.getEditedAt());
        response.setUnsent(message.isUnsent());
        response.setUnsentAt(message.getUnsentAt());
        response.setDeletedForEveryone(message.isDeletedForEveryone());
        response.setDeletedAt(message.getDeletedAt());
        response.setDeletedByUserId(message.getDeletedByUserId());
        response.setPinned(message.isPinned());
        response.setPinnedAt(message.getPinnedAt());
        response.setPinnedByUserId(message.getPinnedByUserId());
        response.setCreatedAt(message.getCreatedAt());
        response.setDeliveredAt(message.getDeliveredAt());
        response.setReadAt(message.getReadAt());
        response.setReplyToMessageId(message.getReplyToMessageId());
        response.setClientMessageId(message.getClientMessageId());
        response.setReactions(null);
        return response;
    }
}



