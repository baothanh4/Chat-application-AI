package org.example.chatapplication.Service;

import lombok.RequiredArgsConstructor;
import org.example.chatapplication.DTO.Request.CreateGroupConversationRequest;
import org.example.chatapplication.DTO.Request.UpdateConversationMuteRequest;
import org.example.chatapplication.DTO.Request.UpdateConversationNicknameRequest;
import org.example.chatapplication.DTO.Request.UpdateConversationSettingsRequest;
import org.example.chatapplication.DTO.Request.CreatePrivateConversationRequest;
import org.example.chatapplication.DTO.Request.UpdateConversationAiConfigRequest;
import org.example.chatapplication.DTO.Response.ChatMessageResponse;
import org.example.chatapplication.DTO.Response.ConversationAiConfigResponse;
import org.example.chatapplication.DTO.Response.ConversationMemberSettingResponse;
import org.example.chatapplication.DTO.Response.ConversationResponse;
import org.example.chatapplication.DTO.Response.ConversationSettingsResponse;
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
        conversation.setAvatarPath(request.getAvatarPath());
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
    public List<Map<String, Object>> listInbox(UUID userId) {
        return conversationRepository.findConversationsByUserId(userId, Pageable.unpaged()).getContent().stream()
                .map(conversation -> buildInboxItem(conversation, userId))
                .toList();
    }

    @Transactional(readOnly = true)
    public Conversation requireConversation(UUID conversationId) {
        return conversationRepository.findById(conversationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Conversation not found: " + conversationId));
    }

    @Transactional(readOnly = true)
    public ConversationSettingsResponse getSettings(UUID conversationId, UUID userId) {
        requireMemberAccess(conversationId, userId);
        Conversation conversation = requireConversation(conversationId);
        return toSettingsResponse(conversation);
    }

    @Transactional
    public ConversationSettingsResponse updateSettings(UUID conversationId, UpdateConversationSettingsRequest request) {
        ConversationMember actor = requireMember(conversationId, request.getUserId());
        Conversation conversation = requireConversation(conversationId);

        boolean touchesGroupIdentity = request.getName() != null || request.getDescription() != null || request.getAvatarPath() != null;
        if (touchesGroupIdentity && conversation.getType() == ConversationType.GROUP && actor.getRole() != ConversationRole.OWNER) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only the group owner can update group identity");
        }

        if (request.getName() != null) {
            String name = request.getName().trim();
            if (name.isEmpty()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Conversation name must not be blank");
            }
            conversation.setName(name);
        }
        if (request.getDescription() != null) {
            conversation.setDescription(trimToNull(request.getDescription()));
        }
        if (request.getAvatarPath() != null) {
            conversation.setAvatarPath(trimToNull(request.getAvatarPath()));
        }
        if (request.getThemeColor() != null) {
            conversation.setThemeColor(trimToNull(request.getThemeColor()));
        }
        if (request.getQuickReactionEmoji() != null) {
            conversation.setQuickReactionEmoji(trimToNull(request.getQuickReactionEmoji()));
        }
        if (request.getReadReceiptEnabled() != null) {
            conversation.setReadReceiptEnabled(request.getReadReceiptEnabled());
        }
        if (request.getDisappearingMessagesSeconds() != null) {
            conversation.setDisappearingMessagesSeconds(request.getDisappearingMessagesSeconds());
        }

        Conversation saved = conversationRepository.save(conversation);
        return toSettingsResponse(saved);
    }

    @Transactional
    public ConversationSettingsResponse updateMemberNickname(UUID conversationId, UUID targetUserId, UpdateConversationNicknameRequest request) {
        ConversationMember actor = requireMember(conversationId, request.getActorUserId());
        ConversationMember target = requireMember(conversationId, targetUserId);

        boolean editingSelf = actor.getUser().getId().equals(targetUserId);
        boolean ownerEditingOthers = actor.getRole() == ConversationRole.OWNER;
        if (!editingSelf && !ownerEditingOthers) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only owner can edit other members' nicknames");
        }

        target.setNickname(trimToNull(request.getNickname()));
        conversationMemberRepository.save(target);
        return toSettingsResponse(target.getConversation());
    }

    @Transactional
    public ConversationSettingsResponse updateMute(UUID conversationId, UpdateConversationMuteRequest request) {
        ConversationMember member = requireMember(conversationId, request.getUserId());
        member.setMuted(request.isMuted());
        conversationMemberRepository.save(member);
        return toSettingsResponse(member.getConversation());
    }

    @Transactional(readOnly = true)
    public ConversationAiConfigResponse getAiConfig(UUID conversationId, UUID userId) {
        requireMemberAccess(conversationId, userId);
        Conversation conversation = requireConversation(conversationId);
        return toAiConfigResponse(conversation, userId);
    }

    @Transactional
    public ConversationAiConfigResponse updateAiConfig(UUID conversationId, UpdateConversationAiConfigRequest request) {
        requireMemberAccess(conversationId, request.getUserId());
        Conversation conversation = requireConversation(conversationId);

        if (request.getSystemPrompt() != null) {
            conversation.setAiSystemPrompt(blankToNull(request.getSystemPrompt()));
        }
        if (request.getBehaviorPrompt() != null) {
            conversation.setAiBehaviorPrompt(blankToNull(request.getBehaviorPrompt()));
        }
        if (request.getTemperature() != null) {
            conversation.setAiTemperature(request.getTemperature());
        }
        if (request.getMaxTokens() != null) {
            conversation.setAiMaxTokens(request.getMaxTokens());
        }
        if (request.getUseOfflineModel() != null) {
            conversation.setAiUseOfflineModel(request.getUseOfflineModel());
        }

        Conversation saved = conversationRepository.save(conversation);
        return toAiConfigResponse(saved, request.getUserId());
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
                conversation.getAvatarPath(),
                members,
                conversation.getCreatedAt(),
                conversation.getUpdatedAt()
        );
    }

    public Map<String, Object> buildInboxItem(Conversation conversation, UUID viewerId) {
        return buildInboxItem(conversation, viewerId, conversationMemberRepository.findByConversationId(conversation.getId()));
    }

    public Map<String, Object> buildInboxItem(Conversation conversation, UUID viewerId, List<ConversationMember> members) {
        Map<String, Object> item = new LinkedHashMap<>();

        List<Map<String, Object>> memberSummaries = new ArrayList<>();
        for (ConversationMember member : members) {
            memberSummaries.add(buildUserMap(member.getUser()));
        }

        ChatMessage latestMessageEntity = chatMessageRepository.findTopByConversationIdOrderByCreatedAtDesc(conversation.getId());
        Map<String, Object> latestMessage = latestMessageEntity == null ? null : buildMessageMap(latestMessageEntity);

        ConversationMember viewerMember = members.stream()
                .filter(member -> member.getUser().getId().equals(viewerId))
                .findFirst()
                .orElse(null);
        Instant lastReadAt = viewerMember == null ? null : viewerMember.getLastReadAt();
        long unreadCount = countUnreadMessages(conversation.getId(), viewerId, lastReadAt);

        item.put("conversationId", conversation.getId());
        item.put("conversationType", conversation.getType());
        item.put("name", conversation.getName());
        item.put("description", conversation.getDescription());
        item.put("archived", conversation.isArchived());
        item.put("avatarPath", conversation.getAvatarPath());
        item.put("themeColor", conversation.getThemeColor());
        item.put("quickReactionEmoji", conversation.getQuickReactionEmoji());
        item.put("readReceiptEnabled", conversation.isReadReceiptEnabled());
        item.put("disappearingMessagesSeconds", conversation.getDisappearingMessagesSeconds());
        item.put("members", memberSummaries);
        item.put("latestMessage", latestMessage);
        item.put("latestSender", latestMessageEntity == null ? null : buildUserMap(latestMessageEntity.getSender()));
        item.put("latestMessageAt", latestMessageEntity == null ? null : latestMessageEntity.getCreatedAt());
        item.put("unreadCount", unreadCount);
        item.put("createdAt", conversation.getCreatedAt());
        item.put("updatedAt", conversation.getUpdatedAt());
        return item;
    }

    private long countUnreadMessages(UUID conversationId, UUID viewerId, Instant lastReadAt) {
        if (lastReadAt == null) {
            return chatMessageRepository.countByConversationIdAndSender_IdNot(conversationId, viewerId);
        }
        return chatMessageRepository.countByConversationIdAndSender_IdNotAndCreatedAtAfter(conversationId, viewerId, lastReadAt);
    }

    private Map<String, Object> buildUserMap(UserAccount user) {
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("id", user.getId());
        summary.put("username", user.getUsername());
        summary.put("displayName", user.getDisplayName());
        summary.put("fullName", user.getFullName());
        summary.put("avatarPath", user.getAvatarPath());
        return summary;
    }

    private Map<String, Object> buildMessageMap(ChatMessage message) {
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("id", message.getId());
        summary.put("conversationId", message.getConversation().getId());
        summary.put("sender", buildUserMap(message.getSender()));
        summary.put("messageType", message.getMessageType());
        summary.put("status", message.getStatus());
        summary.put("content", message.getContent());
        summary.put("clientMessageId", message.getClientMessageId());
        summary.put("createdAt", message.getCreatedAt());
        summary.put("deliveredAt", message.getDeliveredAt());
        summary.put("readAt", message.getReadAt());
        return summary;
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

    private void requireMemberAccess(UUID conversationId, UUID userId) {
        if (userId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "userId is required");
        }
        if (!isMember(conversationId, userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "User is not a member of this conversation");
        }
    }

    private ConversationMember requireMember(UUID conversationId, UUID userId) {
        if (userId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "userId is required");
        }
        return conversationMemberRepository.findByConversationIdAndUserId(conversationId, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "User is not a member of this conversation"));
    }

    private ConversationAiConfigResponse toAiConfigResponse(Conversation conversation, UUID userId) {
        return new ConversationAiConfigResponse(
                conversation.getId(),
                userId,
                conversation.getAiSystemPrompt(),
                conversation.getAiBehaviorPrompt(),
                conversation.getAiTemperature(),
                conversation.getAiMaxTokens(),
                conversation.getAiUseOfflineModel() == null || conversation.getAiUseOfflineModel(),
                conversation.getUpdatedAt()
        );
    }

    private String blankToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private ConversationSettingsResponse toSettingsResponse(Conversation conversation) {
        List<ConversationMemberSettingResponse> members = conversationMemberRepository.findByConversationId(conversation.getId()).stream()
                .map(this::toMemberSettingsResponse)
                .toList();
        return new ConversationSettingsResponse(
                conversation.getId(),
                conversation.getName(),
                conversation.getDescription(),
                conversation.getAvatarPath(),
                conversation.getThemeColor(),
                conversation.getQuickReactionEmoji(),
                conversation.isReadReceiptEnabled(),
                conversation.getDisappearingMessagesSeconds() == null ? 0 : conversation.getDisappearingMessagesSeconds(),
                members,
                conversation.getUpdatedAt()
        );
    }

    private ConversationMemberSettingResponse toMemberSettingsResponse(ConversationMember member) {
        return new ConversationMemberSettingResponse(
                member.getUser().getId(),
                member.getUser().getUsername(),
                member.getUser().getDisplayName(),
                member.getNickname(),
                member.isMuted(),
                member.getLastReadAt()
        );
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
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
