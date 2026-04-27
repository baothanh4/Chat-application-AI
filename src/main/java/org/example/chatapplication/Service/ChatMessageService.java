package org.example.chatapplication.Service;

import lombok.RequiredArgsConstructor;
import org.example.chatapplication.DTO.Request.ReplyMessageRequest;
import org.example.chatapplication.DTO.Request.SendMessageRequest;
import org.example.chatapplication.DTO.Response.ConversationMessagesResponse;
import org.example.chatapplication.DTO.Response.ChatMessageResponse;
import org.example.chatapplication.DTO.Response.MessageReactionSummaryResponse;
import org.example.chatapplication.DTO.Response.MessageHistoryResponse;
import org.example.chatapplication.Model.Entity.ChatMessage;
import org.example.chatapplication.Model.Entity.Conversation;
import org.example.chatapplication.Model.Entity.ConversationMember;
import org.example.chatapplication.Model.Entity.MessageReaction;
import org.example.chatapplication.Model.Entity.UserAccount;
import org.example.chatapplication.Model.Enum.MessageStatus;
import org.example.chatapplication.Model.Enum.MessageType;
import org.example.chatapplication.Repository.ChatMessageRepository;
import org.example.chatapplication.Repository.ConversationMemberRepository;
import org.example.chatapplication.Repository.MessageReactionRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ChatMessageService {
    private final ChatMessageRepository chatMessageRepository;
    private final ConversationService conversationService;
    private final UserAccountService userAccountService;
    private final MessageQueueService messageQueueService;
    private final ConversationMemberRepository conversationMemberRepository;
    private final MessageReactionRepository messageReactionRepository;

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

        return createAndPublishMessage(
                conversation,
                sender,
                request.getContent().trim(),
                request.getMessageType() == null ? MessageType.TEXT : request.getMessageType(),
                request.getClientMessageId(),
                MessageStatus.SENT,
                null
        );
    }

    @Transactional
    public ChatMessageResponse sendSystemMessage(UUID conversationId, UUID actorUserId, String content) {
        Conversation conversation = conversationService.requireConversation(conversationId);
        if (!conversationService.isMember(conversationId, actorUserId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "User is not a member of this conversation");
        }
        UserAccount actor = userAccountService.requireUser(actorUserId);
        return createAndPublishMessage(
                conversation,
                actor,
                content,
                MessageType.SYSTEM,
                "system-" + UUID.randomUUID(),
                MessageStatus.DELIVERED,
                null
        );
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

    @Transactional
    public ChatMessageResponse pinMessage(UUID messageId, UUID actorUserId) {
        ChatMessage message = requireMessage(messageId);
        requireConversationMembership(message.getConversation().getId(), actorUserId);

        message.setPinned(true);
        message.setPinnedAt(Instant.now());
        message.setPinnedByUserId(actorUserId);

        return toResponse(chatMessageRepository.save(message));
    }

    @Transactional
    public ChatMessageResponse unpinMessage(UUID messageId, UUID actorUserId) {
        ChatMessage message = requireMessage(messageId);
        requireConversationMembership(message.getConversation().getId(), actorUserId);

        message.setPinned(false);
        message.setPinnedAt(null);
        message.setPinnedByUserId(null);

        return toResponse(chatMessageRepository.save(message));
    }

    @Transactional(readOnly = true)
    public List<ChatMessageResponse> listPinnedMessages(UUID conversationId, UUID actorUserId) {
        requireConversationMembership(conversationId, actorUserId);
        return chatMessageRepository.findByConversationIdAndPinnedTrueOrderByPinnedAtDescCreatedAtDesc(conversationId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public ChatMessageResponse addOrUpdateReaction(UUID messageId, UUID actorUserId, String emoji) {
        String normalizedEmoji = emoji == null ? null : emoji.trim();
        if (normalizedEmoji == null || normalizedEmoji.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Emoji is required");
        }

        ChatMessage message = requireMessage(messageId);
        requireConversationMembership(message.getConversation().getId(), actorUserId);
        UserAccount actor = userAccountService.requireUser(actorUserId);

        MessageReaction reaction = messageReactionRepository.findByMessageIdAndUserId(messageId, actorUserId)
                .orElseGet(() -> {
                    MessageReaction created = new MessageReaction();
                    created.setMessage(message);
                    created.setUser(actor);
                    return created;
                });
        reaction.setEmoji(normalizedEmoji);
        messageReactionRepository.save(reaction);

        return toResponse(message);
    }

    @Transactional
    public ChatMessageResponse removeReaction(UUID messageId, UUID actorUserId) {
        ChatMessage message = requireMessage(messageId);
        requireConversationMembership(message.getConversation().getId(), actorUserId);
        messageReactionRepository.deleteByMessageIdAndUserId(messageId, actorUserId);
        return toResponse(message);
    }

    @Transactional
    public ChatMessageResponse replyMessage(UUID targetMessageId, ReplyMessageRequest request) {
        ChatMessage targetMessage = requireMessage(targetMessageId);
        UUID actorUserId = request.getActorUserId();
        UUID conversationId = targetMessage.getConversation().getId();

        requireConversationMembership(conversationId, actorUserId);

        UserAccount actor = userAccountService.requireUser(actorUserId);
        String content = normalizeMessageContent(request.getContent());

        return createAndPublishMessage(
                targetMessage.getConversation(),
                actor,
                content,
                MessageType.TEXT,
                request.getClientMessageId(),
                MessageStatus.SENT,
                targetMessage.getId()
        );
    }

    @Transactional
    public ChatMessageResponse editMessage(UUID messageId, UUID actorUserId, String content) {
        ChatMessage message = requireMessage(messageId);
        requireConversationMembership(message.getConversation().getId(), actorUserId);
        requireSenderPermission(message, actorUserId);

        if (message.isUnsent() || message.isDeletedForEveryone()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Cannot edit an unsent or deleted message");
        }

        message.setContent(normalizeMessageContent(content));
        message.setEdited(true);
        message.setEditedAt(Instant.now());

        return toResponse(chatMessageRepository.save(message));
    }

    @Transactional
    public ChatMessageResponse unsendMessage(UUID messageId, UUID actorUserId) {
        ChatMessage message = requireMessage(messageId);
        requireConversationMembership(message.getConversation().getId(), actorUserId);
        requireSenderPermission(message, actorUserId);

        if (message.isDeletedForEveryone()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Message has already been deleted");
        }

        message.setUnsent(true);
        message.setUnsentAt(Instant.now());
        message.setContent("Message was unsent");

        return toResponse(chatMessageRepository.save(message));
    }

    @Transactional
    public ChatMessageResponse deleteMessageForEveryone(UUID messageId, UUID actorUserId) {
        ChatMessage message = requireMessage(messageId);
        requireConversationMembership(message.getConversation().getId(), actorUserId);
        requireSenderPermission(message, actorUserId);

        message.setDeletedForEveryone(true);
        message.setDeletedAt(Instant.now());
        message.setDeletedByUserId(actorUserId);
        message.setContent("Message was deleted");

        return toResponse(chatMessageRepository.save(message));
    }

    @Transactional(readOnly = true)
    public MessageHistoryResponse getConversationMedia(UUID conversationId, UUID actorUserId, MessageType type, int page, int size) {
        requireConversationMembership(conversationId, actorUserId);
        if (type != MessageType.IMAGE && type != MessageType.VIDEO && type != MessageType.FILE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Media tab supports IMAGE, VIDEO, FILE only");
        }

        Conversation conversation = conversationService.requireConversation(conversationId);
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<ChatMessage> messagePage = chatMessageRepository
                .findByConversationIdAndMessageTypeAndUnsentFalseAndDeletedForEveryoneFalseOrderByCreatedAtDesc(
                        conversationId,
                        type,
                        pageable
                );

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

    @Transactional(readOnly = true)
    public ChatMessageResponse toResponse(ChatMessage message) {
        List<MessageReactionSummaryResponse> reactions = summarizeReactions(message.getId());
        return new ChatMessageResponse(
                message.getId(),
                message.getConversation().getId(),
                userAccountService.toResponse(message.getSender()),
                message.getMessageType(),
                message.getStatus(),
                message.getContent(),
                message.getClientMessageId(),
                message.getReplyToMessageId(),
                message.isEdited(),
                message.getEditedAt(),
                message.isUnsent(),
                message.getUnsentAt(),
                message.isDeletedForEveryone(),
                message.getDeletedAt(),
                message.getDeletedByUserId(),
                message.isPinned(),
                message.getPinnedAt(),
                message.getPinnedByUserId(),
                reactions,
                message.getCreatedAt(),
                message.getDeliveredAt(),
                message.getReadAt()
        );
    }

    private ChatMessageResponse createAndPublishMessage(Conversation conversation,
                                                        UserAccount sender,
                                                        String content,
                                                        MessageType type,
                                                        String clientMessageId,
                                                        MessageStatus status,
                                                        UUID replyToMessageId) {
        ChatMessage message = new ChatMessage();
        message.setConversation(conversation);
        message.setSender(sender);
        message.setContent(content);
        message.setMessageType(type);
        message.setClientMessageId(clientMessageId);
        message.setStatus(status);
        message.setReplyToMessageId(replyToMessageId);

        ChatMessage saved = chatMessageRepository.save(message);
        messageQueueService.publishMessageCreated(saved);
        return toResponse(saved);
    }

    private ChatMessage requireMessage(UUID messageId) {
        return chatMessageRepository.findById(messageId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Message not found: " + messageId));
    }

    private void requireConversationMembership(UUID conversationId, UUID userId) {
        if (userId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "actorUserId is required");
        }
        if (!conversationService.isMember(conversationId, userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "User is not a member of this conversation");
        }
    }

    private void requireSenderPermission(ChatMessage message, UUID actorUserId) {
        if (!message.getSender().getId().equals(actorUserId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only message sender can perform this action");
        }
    }

    private String normalizeMessageContent(String content) {
        if (content == null || content.trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Message content is required");
        }
        return content.trim();
    }

    private List<MessageReactionSummaryResponse> summarizeReactions(UUID messageId) {
        List<MessageReaction> reactions = messageReactionRepository.findByMessageIdOrderByCreatedAtAsc(messageId);
        if (reactions.isEmpty()) {
            return List.of();
        }

        Map<String, List<UUID>> grouped = new LinkedHashMap<>();
        for (MessageReaction reaction : reactions) {
            grouped.computeIfAbsent(reaction.getEmoji(), ignored -> new ArrayList<>()).add(reaction.getUser().getId());
        }

        return grouped.entrySet().stream()
                .map(entry -> new MessageReactionSummaryResponse(entry.getKey(), entry.getValue().size(), entry.getValue()))
                .toList();
    }
}
