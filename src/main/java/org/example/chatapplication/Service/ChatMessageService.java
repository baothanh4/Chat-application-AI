package org.example.chatapplication.Service;

import lombok.RequiredArgsConstructor;
import org.example.chatapplication.DTO.Request.SendMessageRequest;
import org.example.chatapplication.DTO.Response.ConversationMessagesResponse;
import org.example.chatapplication.DTO.Response.ChatMessageResponse;
import org.example.chatapplication.DTO.Response.MessageHistoryResponse;
import org.example.chatapplication.Model.Entity.ChatMessage;
import org.example.chatapplication.Model.Entity.Conversation;
import org.example.chatapplication.Model.Entity.ConversationMember;
import org.example.chatapplication.Model.Entity.UserAccount;
import org.example.chatapplication.Model.Enum.MessageStatus;
import org.example.chatapplication.Model.Enum.MessageType;
import org.example.chatapplication.Repository.ChatMessageRepository;
import org.example.chatapplication.Repository.ConversationMemberRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ChatMessageService {
    private final ChatMessageRepository chatMessageRepository;
    private final ConversationService conversationService;
    private final UserAccountService userAccountService;
    private final MessageQueueService messageQueueService;
    private final ConversationMemberRepository conversationMemberRepository;

    @Transactional
    public ChatMessageResponse sendMessage(SendMessageRequest request) {
        UserAccount sender = userAccountService.requireUser(request.getSenderId());
        Conversation conversation;

        if (request.getConversationId() != null) {
            conversation = conversationService.requireConversation(request.getConversationId());

            if (!conversationService.isMember(conversation.getId(), sender.getId())) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Sender is not a member of this conversation");
            }
        } else {
            conversation = conversationService.resolveOrCreatePrivateConversation(sender.getId(), request.getRecipientId());
        }

        ChatMessage message = new ChatMessage();
        message.setConversation(conversation);
        message.setSender(sender);
        message.setContent(request.getContent().trim());
        message.setMessageType(request.getMessageType() == null ? MessageType.TEXT : request.getMessageType());
        message.setClientMessageId(request.getClientMessageId());
        message.setStatus(MessageStatus.SENT);

        ChatMessage saved = chatMessageRepository.save(message);
        messageQueueService.publishMessageCreated(saved);
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public ConversationMessagesResponse getConversationMessages(UUID conversationId) {
        Conversation conversation = conversationService.requireConversation(conversationId);
        Page<ChatMessage> messagePage = chatMessageRepository.findByConversationIdOrderByCreatedAtDesc(conversationId, Pageable.unpaged());
        List<ChatMessageResponse> messages = messagePage.getContent().stream()
                .sorted(Comparator.comparing(ChatMessage::getCreatedAt))
                .map(this::toResponse)
                .toList();
        return new ConversationMessagesResponse(
                conversationId,
                conversationService.toResponse(conversation),
                messages
        );
    }

    @Transactional(readOnly = true)
    public MessageHistoryResponse getHistory(UUID conversationId, int page, int size) {
        Conversation conversation = conversationService.requireConversation(conversationId);
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<ChatMessage> messagePage = chatMessageRepository.findByConversationIdOrderByCreatedAtDesc(conversationId, pageable);
        List<ChatMessageResponse> messages = messagePage.getContent().stream()
                .sorted(Comparator.comparing(ChatMessage::getCreatedAt))
                .map(this::toResponse)
                .toList();
        return new MessageHistoryResponse(
                conversationId,
                conversationService.toResponse(conversation),
                messages,
                page,
                size,
                messagePage.getTotalElements(),
                messagePage.getTotalPages()
        );
    }

    @Transactional
    public ChatMessage markDelivered(ChatMessage message) {
        if (message.getDeliveredAt() == null) {
            message.setDeliveredAt(Instant.now());
            if (message.getStatus() == MessageStatus.SENT) {
                message.setStatus(MessageStatus.DELIVERED);
            }
            return chatMessageRepository.save(message);
        }
        return message;
    }

    @Transactional
    public ChatMessage markRead(UUID conversationId, UUID userId) {
        ConversationMember member = conversationMemberRepository.findByConversationIdAndUserId(conversationId, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Conversation member not found"));
        member.setLastReadAt(Instant.now());
        conversationMemberRepository.save(member);
        return chatMessageRepository.findByConversationIdOrderByCreatedAtDesc(conversationId, PageRequest.of(0, 1))
                .stream()
                .findFirst()
                .map(message -> {
                    message.setReadAt(Instant.now());
                    message.setStatus(MessageStatus.READ);
                    return chatMessageRepository.save(message);
                })
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No message found for conversation"));
    }

    @Transactional(readOnly = true)
    public ChatMessageResponse toResponse(ChatMessage message) {
        return new ChatMessageResponse(
                message.getId(),
                message.getConversation().getId(),
                userAccountService.toResponse(message.getSender()),
                message.getMessageType(),
                message.getStatus(),
                message.getContent(),
                message.getClientMessageId(),
                message.getCreatedAt(),
                message.getDeliveredAt(),
                message.getReadAt()
        );
    }
}
