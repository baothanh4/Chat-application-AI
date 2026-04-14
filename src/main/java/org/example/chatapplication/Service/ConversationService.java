package org.example.chatapplication.Service;

import lombok.RequiredArgsConstructor;
import org.example.chatapplication.DTO.Request.CreateGroupConversationRequest;
import org.example.chatapplication.DTO.Request.CreatePrivateConversationRequest;
import org.example.chatapplication.DTO.Response.ChatMessageResponse;
import org.example.chatapplication.DTO.Response.ConversationInboxItemResponse;
import org.example.chatapplication.DTO.Response.ConversationResponse;
import org.example.chatapplication.DTO.Response.UserResponse;
import org.example.chatapplication.Model.Entity.ChatMessage;
import org.example.chatapplication.Model.Entity.Conversation;
import org.example.chatapplication.Model.Entity.ConversationMember;
import org.example.chatapplication.Model.Entity.UserAccount;
import org.example.chatapplication.Model.Enum.ConversationRole;
import org.example.chatapplication.Model.Enum.ConversationType;
import org.example.chatapplication.Repository.ChatMessageRepository;
import org.example.chatapplication.Repository.ConversationMemberRepository;
import org.example.chatapplication.Repository.ConversationRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.*;

@Service
@RequiredArgsConstructor
public class ConversationService {
    private final ConversationRepository conversationRepository;
    private final ConversationMemberRepository conversationMemberRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final UserAccountService userAccountService;

    @Transactional
    public ConversationResponse createPrivateConversation(CreatePrivateConversationRequest request) {
        if (request.getOwnerId().equals(request.getRecipientId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Private chat requires two different users");
        }

        Conversation conversation = resolveOrCreatePrivateConversation(request.getOwnerId(), request.getRecipientId());
        return toResponse(conversation);
    }

    @Transactional
    public Conversation resolveOrCreatePrivateConversation(UUID ownerId, UUID recipientId) {
        if (ownerId.equals(recipientId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Private chat requires two different users");
        }

        UserAccount owner = userAccountService.requireUser(ownerId);
        UserAccount recipient = userAccountService.requireUser(recipientId);

        List<Conversation> existingThreads = conversationRepository.findPrivateConversationBetweenUsers(owner.getId(), recipient.getId());
        if (!existingThreads.isEmpty()) {
            return existingThreads.getFirst();
        }

        Conversation conversation = new Conversation();
        conversation.setType(ConversationType.PRIVATE);
        conversation.setName("private-chat");
        conversation = conversationRepository.save(conversation);

        addMember(conversation, owner, ConversationRole.OWNER);
        addMember(conversation, recipient, ConversationRole.MEMBER);

        return conversation;
    }

    @Transactional
    public ConversationResponse createGroupConversation(CreateGroupConversationRequest request) {
        UserAccount owner = userAccountService.requireUser(request.getOwnerId());
        Set<UUID> memberIds = new LinkedHashSet<>(request.getMemberIds());
        memberIds.add(request.getOwnerId());

        if (memberIds.size() < 2) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Group chat requires at least two members");
        }

        Conversation conversation = new Conversation();
        conversation.setType(ConversationType.GROUP);
        conversation.setName(request.getName().trim());
        conversation.setDescription(request.getDescription());
        conversation = conversationRepository.save(conversation);

        for (UUID memberId : memberIds) {
            UserAccount user = userAccountService.requireUser(memberId);
            addMember(conversation, user, memberId.equals(owner.getId()) ? ConversationRole.OWNER : ConversationRole.MEMBER);
        }

        return toResponse(conversation);
    }

    @Transactional(readOnly = true)
    public Page<ConversationResponse> listConversations(UUID userId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "updatedAt"));
        return conversationRepository.findConversationsByUserId(userId, pageable).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public List<ConversationInboxItemResponse> listInbox(UUID userId) {
        return conversationRepository.findConversationsByUserId(userId, Pageable.unpaged()).getContent().stream()
                .map(conversation -> toInboxItem(conversation, userId))
                .toList();
    }

    @Transactional(readOnly = true)
    public Conversation requireConversation(UUID conversationId) {
        return conversationRepository.findById(conversationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Conversation not found: " + conversationId));
    }

    @Transactional(readOnly = true)
    public ConversationResponse toResponse(Conversation conversation) {
        Set<UserResponse> members = new LinkedHashSet<>();
        for (ConversationMember member : conversationMemberRepository.findByConversationId(conversation.getId())) {
            members.add(userAccountService.toResponse(member.getUser()));
        }
        return new ConversationResponse(
                conversation.getId(),
                conversation.getType(),
                conversation.getName(),
                conversation.getDescription(),
                conversation.isArchived(),
                members,
                conversation.getCreatedAt(),
                conversation.getUpdatedAt()
        );
    }

    @Transactional(readOnly = true)
    public ConversationInboxItemResponse toInboxItem(Conversation conversation, UUID viewerId) {
        Set<UserResponse> members = new LinkedHashSet<>();
        for (ConversationMember member : conversationMemberRepository.findByConversationId(conversation.getId())) {
            members.add(userAccountService.toResponse(member.getUser()));
        }

        ChatMessage latestMessageEntity = chatMessageRepository.findTopByConversationIdOrderByCreatedAtDesc(conversation.getId());
        ChatMessageResponse latestMessage = latestMessageEntity == null ? null : new ChatMessageResponse(
                latestMessageEntity.getId(),
                latestMessageEntity.getConversation().getId(),
                userAccountService.toResponse(latestMessageEntity.getSender()),
                latestMessageEntity.getMessageType(),
                latestMessageEntity.getStatus(),
                latestMessageEntity.getContent(),
                latestMessageEntity.getClientMessageId(),
                latestMessageEntity.getCreatedAt(),
                latestMessageEntity.getDeliveredAt(),
                latestMessageEntity.getReadAt()
        );

        ConversationMember viewerMember = conversationMemberRepository.findByConversationIdAndUserId(conversation.getId(), viewerId).orElse(null);
        java.time.Instant lastReadAt = viewerMember == null ? null : viewerMember.getLastReadAt();
        long unreadCount = chatMessageRepository.findByConversationIdOrderByCreatedAtDesc(conversation.getId(), Pageable.unpaged()).stream()
                .filter(message -> !message.getSender().getId().equals(viewerId))
                .filter(message -> lastReadAt == null || message.getCreatedAt().isAfter(lastReadAt))
                .count();

        return new ConversationInboxItemResponse(
                conversation.getId(),
                conversation.getType(),
                conversation.getName(),
                conversation.getDescription(),
                conversation.isArchived(),
                members,
                latestMessage,
                latestMessage == null ? null : latestMessage.getSender(),
                latestMessage == null ? null : latestMessage.getCreatedAt(),
                unreadCount,
                conversation.getCreatedAt(),
                conversation.getUpdatedAt()
        );
    }

    @Transactional(readOnly = true)
    List<UserAccount> getConversationMembers(UUID conversationId) {
        List<UserAccount> users = new ArrayList<>();
        for (ConversationMember member : conversationMemberRepository.findByConversationId(conversationId)) {
            users.add(member.getUser());
        }
        return users;
    }

    @Transactional(readOnly = true)
    boolean isMember(UUID conversationId, UUID userId) {
        return conversationMemberRepository.existsByConversationIdAndUserId(conversationId, userId);
    }

    private void addMember(Conversation conversation, UserAccount user, ConversationRole role) {
        ConversationMember member = new ConversationMember();
        member.setConversation(conversation);
        member.setUser(user);
        member.setRole(role);
        conversation.getMembers().add(member);
        conversationMemberRepository.save(member);
    }
}
