package org.example.chatapplication.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.chatapplication.ChatMessageCreatedEvent;
import org.example.chatapplication.DTO.Response.ConversationAiInsightResponse;
import org.example.chatapplication.Model.Entity.ChatMessage;
import org.example.chatapplication.Model.Entity.Conversation;
import org.example.chatapplication.Model.Entity.ConversationMember;
import org.example.chatapplication.Model.Enum.MessageStatus;
import org.example.chatapplication.Repository.ChatMessageRepository;
import org.example.chatapplication.Repository.ConversationMemberRepository;
import org.example.chatapplication.Repository.ConversationRepository;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChatMessageDeliveryListener {
    private final ChatMessageRepository chatMessageRepository;
    private final ConversationRepository conversationRepository;
    private final ConversationMemberRepository conversationMemberRepository;
    private final PresenceService presenceService;
    private final NotificationService notificationService;
    private final ConversationAiService conversationAiService;
    private final SimpMessagingTemplate messagingTemplate;

    @Async("chatTaskExecutor")
    @TransactionalEventListener(phase = org.springframework.transaction.event.TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onMessageCreated(ChatMessageCreatedEvent event) {
        ChatMessage message = chatMessageRepository.findById(event.messageId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Message not found: " + event.messageId()));
        Conversation conversation = conversationRepository.findById(event.conversationId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Conversation not found: " + event.conversationId()));

        boolean atLeastOneRecipientOnline = false;
        for (ConversationMember member : conversationMemberRepository.findByConversationId(conversation.getId())) {
            if (member.getUser().getId().equals(event.senderId())) {
                continue;
            }

            if (presenceService.isOnline(member.getUser().getId())) {
                atLeastOneRecipientOnline = true;
            } else {
                notificationService.sendOfflineMessageNotification(member.getUser(), message, conversation);
            }
        }

        if (atLeastOneRecipientOnline) {
            message.setDeliveredAt(Instant.now());
            if (message.getStatus() == MessageStatus.SENT) {
                message.setStatus(MessageStatus.DELIVERED);
            }
            chatMessageRepository.save(message);
        }

        for (ConversationMember member : conversationMemberRepository.findByConversationId(conversation.getId())) {
            Map<String, Object> summary = buildInboxSummary(conversation, member.getUser().getId());
            messagingTemplate.convertAndSend("/topic/users/" + member.getUser().getId() + "/inbox", (Object) summary);
        }

        try {
            ConversationAiInsightResponse insight = conversationAiService.refreshConversationInsight(conversation.getId());
            messagingTemplate.convertAndSend("/topic/conversations/" + conversation.getId() + "/ai", insight);

            for (ConversationMember member : conversationMemberRepository.findByConversationId(conversation.getId())) {
                messagingTemplate.convertAndSend("/topic/users/" + member.getUser().getId() + "/ai", insight);
            }
        } catch (Exception ex) {
            log.warn("AI insight refresh failed for conversation {}: {}", conversation.getId(), ex.getMessage());
        }

        log.debug("Message {} processed for conversation {}", message.getId(), conversation.getId());
    }

    private Map<String, Object> buildInboxSummary(Conversation conversation, UUID viewerId) {
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("conversationId", conversation.getId());
        summary.put("conversationType", conversation.getType());
        summary.put("name", conversation.getName());
        summary.put("description", conversation.getDescription());
        summary.put("archived", conversation.isArchived());

        List<Map<String, Object>> members = conversationMemberRepository.findByConversationId(conversation.getId()).stream()
                .map(member -> userSummary(member.getUser()))
                .toList();
        summary.put("members", members);

        ChatMessage latestMessageEntity = chatMessageRepository.findTopByConversationIdOrderByCreatedAtDesc(conversation.getId());
        Map<String, Object> latestMessage = latestMessageEntity == null ? null : messageSummary(latestMessageEntity);
        summary.put("latestMessage", latestMessage);
        summary.put("latestSender", latestMessageEntity == null ? null : userSummary(latestMessageEntity.getSender()));
        summary.put("latestMessageAt", latestMessageEntity == null ? null : latestMessageEntity.getCreatedAt());

        ConversationMember viewerMember = conversationMemberRepository.findByConversationIdAndUserId(conversation.getId(), viewerId).orElse(null);
        Instant lastReadAt = viewerMember == null ? null : viewerMember.getLastReadAt();
        long unreadCount = chatMessageRepository.findByConversationIdOrderByCreatedAtDesc(conversation.getId(), org.springframework.data.domain.Pageable.unpaged()).stream()
                .filter(message -> !message.getSender().getId().equals(viewerId))
                .filter(message -> lastReadAt == null || message.getCreatedAt().isAfter(lastReadAt))
                .count();

        summary.put("unreadCount", unreadCount);
        summary.put("createdAt", conversation.getCreatedAt());
        summary.put("updatedAt", conversation.getUpdatedAt());
        return summary;
    }

    private Map<String, Object> userSummary(org.example.chatapplication.Model.Entity.UserAccount user) {
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("id", user.getId());
        summary.put("username", user.getUsername());
        summary.put("displayName", user.getDisplayName());
        summary.put("avatarPath", user.getAvatarPath());
        return summary;
    }

    private Map<String, Object> messageSummary(ChatMessage message) {
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("id", message.getId());
        summary.put("conversationId", message.getConversation().getId());
        summary.put("sender", userSummary(message.getSender()));
        summary.put("messageType", message.getMessageType());
        summary.put("status", message.getStatus());
        summary.put("content", message.getContent());
        summary.put("clientMessageId", message.getClientMessageId());
        summary.put("createdAt", message.getCreatedAt());
        summary.put("deliveredAt", message.getDeliveredAt());
        summary.put("readAt", message.getReadAt());
        return summary;
    }
}
