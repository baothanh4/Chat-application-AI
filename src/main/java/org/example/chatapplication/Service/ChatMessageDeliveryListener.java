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
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChatMessageDeliveryListener {
    private final ChatMessageRepository chatMessageRepository;
    private final ConversationRepository conversationRepository;
    private final ConversationMemberRepository conversationMemberRepository;
    private final ConversationService conversationService;
    private final PresenceService presenceService;
    private final NotificationService notificationService;
    private final ConversationAiService conversationAiService;
    private final AiBotService aiBotService;
    private final SimpMessagingTemplate messagingTemplate;

    @Async("chatTaskExecutor")
    @TransactionalEventListener(phase = org.springframework.transaction.event.TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onMessageCreated(ChatMessageCreatedEvent event) {
        ChatMessage message = chatMessageRepository.findById(event.messageId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Message not found: " + event.messageId()));
        Conversation conversation = conversationRepository.findById(event.conversationId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Conversation not found: " + event.conversationId()));
        List<ConversationMember> members = conversationMemberRepository.findByConversationId(conversation.getId());

        boolean atLeastOneRecipientOnline = false;
        for (ConversationMember member : members) {
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

        for (ConversationMember member : members) {
            var summary = conversationService.buildInboxItem(conversation, member.getUser().getId(), members);
            messagingTemplate.convertAndSend("/topic/users/" + member.getUser().getId() + "/inbox", (Object) summary);
        }

        try {
            ConversationAiInsightResponse insight = conversationAiService.refreshConversationInsight(conversation.getId());
            messagingTemplate.convertAndSend("/topic/conversations/" + conversation.getId() + "/ai", insight);

            for (ConversationMember member : members) {
                messagingTemplate.convertAndSend("/topic/users/" + member.getUser().getId() + "/ai", insight);
            }
        } catch (Exception ex) {
            log.warn("AI insight refresh failed for conversation {}: {}", conversation.getId(), ex.getMessage());
        }

        // ── Bot reply: nếu có AI Bot là thành viên và người gửi không phải bot ──
        try {
            UUID botId = aiBotService.getBotUserId();
            if (botId != null && !event.senderId().equals(botId)) {
                boolean botIsMember = members.stream()
                        .anyMatch(m -> botId.equals(m.getUser().getId()));

                if (botIsMember) {
                    String userMessage = message.getContent();
                    String botReplyText = aiBotService.generateBotReply(userMessage, conversation.getId());

                    // Tạo message từ bot
                    ChatMessage botMessage = new ChatMessage();
                    botMessage.setConversation(conversation);
                    botMessage.setSender(members.stream()
                            .filter(m -> botId.equals(m.getUser().getId()))
                            .findFirst()
                            .map(org.example.chatapplication.Model.Entity.ConversationMember::getUser)
                            .orElseThrow());
                    botMessage.setContent(botReplyText);
                    botMessage.setMessageType(org.example.chatapplication.Model.Enum.MessageType.TEXT);
                    botMessage.setStatus(org.example.chatapplication.Model.Enum.MessageStatus.DELIVERED);
                    botMessage.setClientMessageId("bot-" + java.util.UUID.randomUUID());
                    ChatMessage savedBot = chatMessageRepository.save(botMessage);

                    // Map sang response DTO thủ công
                    java.util.Map<String, Object> botSender = new java.util.LinkedHashMap<>();
                    botSender.put("id", savedBot.getSender().getId());
                    botSender.put("username", savedBot.getSender().getUsername());
                    botSender.put("displayName", savedBot.getSender().getDisplayName());
                    botSender.put("avatarPath", savedBot.getSender().getAvatarPath());

                    java.util.Map<String, Object> botMsgMap = new java.util.LinkedHashMap<>();
                    botMsgMap.put("id", savedBot.getId());
                    botMsgMap.put("conversationId", conversation.getId());
                    botMsgMap.put("sender", botSender);
                    botMsgMap.put("messageType", savedBot.getMessageType());
                    botMsgMap.put("status", savedBot.getStatus());
                    botMsgMap.put("content", savedBot.getContent());
                    botMsgMap.put("clientMessageId", savedBot.getClientMessageId());
                    botMsgMap.put("createdAt", savedBot.getCreatedAt());
                    botMsgMap.put("deliveredAt", savedBot.getDeliveredAt());
                    botMsgMap.put("readAt", savedBot.getReadAt());

                    // Gửi reply qua STOMP
                    messagingTemplate.convertAndSend("/topic/conversations/" + conversation.getId(), (Object) botMsgMap);

                    // Cập nhật inbox cho tất cả members
                    for (ConversationMember member : members) {
                        java.util.Map<String, Object> summary = conversationService.buildInboxItem(conversation, member.getUser().getId(), members);
                        messagingTemplate.convertAndSend("/topic/users/" + member.getUser().getId() + "/inbox", (Object) summary);
                    }
                }
            }
        } catch (Exception ex) {
            log.warn("Bot reply failed for conversation {}: {}", conversation.getId(), ex.getMessage());
        }

        log.debug("Message {} processed for conversation {}", message.getId(), conversation.getId());
    }
}
