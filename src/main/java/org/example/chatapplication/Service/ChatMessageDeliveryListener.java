package org.example.chatapplication.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.chatapplication.ChatMessageCreatedEvent;
import org.example.chatapplication.DTO.Response.ConversationInboxItemResponse;
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

@Service
@RequiredArgsConstructor
@Slf4j
public class ChatMessageDeliveryListener {
    private final ChatMessageRepository chatMessageRepository;
    private final ConversationRepository conversationRepository;
    private final ConversationMemberRepository conversationMemberRepository;
    private final PresenceService presenceService;
    private final NotificationService notificationService;
    private final ConversationService conversationService;
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
            ConversationInboxItemResponse summary = conversationService.toInboxItem(conversation, member.getUser().getId());
            messagingTemplate.convertAndSend("/topic/users/" + member.getUser().getId() + "/inbox", summary);
        }

        log.debug("Message {} processed for conversation {}", message.getId(), conversation.getId());
    }
}
