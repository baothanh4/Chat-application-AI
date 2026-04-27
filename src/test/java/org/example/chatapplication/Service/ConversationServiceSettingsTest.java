package org.example.chatapplication.Service;

import org.example.chatapplication.DTO.Request.UpdateConversationMuteRequest;
import org.example.chatapplication.DTO.Request.UpdateConversationNicknameRequest;
import org.example.chatapplication.DTO.Request.UpdateConversationSettingsRequest;
import org.example.chatapplication.DTO.Response.ConversationSettingsResponse;
import org.example.chatapplication.Model.Entity.Conversation;
import org.example.chatapplication.Model.Entity.ConversationMember;
import org.example.chatapplication.Model.Entity.UserAccount;
import org.example.chatapplication.Model.Enum.ConversationRole;
import org.example.chatapplication.Model.Enum.ConversationType;
import org.example.chatapplication.Repository.ChatMessageRepository;
import org.example.chatapplication.Repository.ConversationMemberRepository;
import org.example.chatapplication.Repository.ConversationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConversationServiceSettingsTest {

    @Mock
    private ConversationRepository conversationRepository;
    @Mock
    private ConversationMemberRepository conversationMemberRepository;
    @Mock
    private ChatMessageRepository chatMessageRepository;
    @Mock
    private UserAccountService userAccountService;

    private ConversationService conversationService;

    @BeforeEach
    void setUp() {
        conversationService = new ConversationService(
                conversationRepository,
                conversationMemberRepository,
                chatMessageRepository,
                userAccountService
        );
    }

    @Test
    void updateSettingsShouldRejectNonOwnerChangingGroupName() {
        UUID conversationId = UUID.randomUUID();
        UUID actorId = UUID.randomUUID();

        Conversation conversation = conversation(conversationId, ConversationType.GROUP);
        ConversationMember actorMember = member(conversation, actorId, ConversationRole.MEMBER);

        when(conversationMemberRepository.findByConversationIdAndUserId(conversationId, actorId)).thenReturn(Optional.of(actorMember));
        when(conversationRepository.findById(conversationId)).thenReturn(Optional.of(conversation));

        UpdateConversationSettingsRequest request = new UpdateConversationSettingsRequest(
                actorId,
                "New Group Name",
                null,
                null,
                null,
                null,
                null,
                null
        );

        assertThatThrownBy(() -> conversationService.updateSettings(conversationId, request))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN));
    }

    @Test
    void updateSettingsShouldPersistThemeAndDisappearingMode() {
        UUID conversationId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();

        Conversation conversation = conversation(conversationId, ConversationType.GROUP);
        ConversationMember ownerMember = member(conversation, ownerId, ConversationRole.OWNER);

        when(conversationMemberRepository.findByConversationIdAndUserId(conversationId, ownerId)).thenReturn(Optional.of(ownerMember));
        when(conversationRepository.findById(conversationId)).thenReturn(Optional.of(conversation));
        when(conversationRepository.save(any(Conversation.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(conversationMemberRepository.findByConversationId(conversationId)).thenReturn(List.of(ownerMember));

        UpdateConversationSettingsRequest request = new UpdateConversationSettingsRequest(
                ownerId,
                "Core Team",
                null,
                null,
                "#ff00aa",
                "❤️",
                Boolean.FALSE,
                3600
        );

        ConversationSettingsResponse response = conversationService.updateSettings(conversationId, request);

        assertThat(response.getName()).isEqualTo("Core Team");
        assertThat(response.getThemeColor()).isEqualTo("#ff00aa");
        assertThat(response.getQuickReactionEmoji()).isEqualTo("❤️");
        assertThat(response.isReadReceiptEnabled()).isFalse();
        assertThat(response.getDisappearingMessagesSeconds()).isEqualTo(3600);
    }

    @Test
    void updateMuteShouldPersistMemberMuteState() {
        UUID conversationId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        Conversation conversation = conversation(conversationId, ConversationType.PRIVATE);
        ConversationMember member = member(conversation, userId, ConversationRole.MEMBER);
        member.setMuted(false);

        when(conversationMemberRepository.findByConversationIdAndUserId(conversationId, userId)).thenReturn(Optional.of(member));
        when(conversationMemberRepository.save(any(ConversationMember.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(conversationMemberRepository.findByConversationId(conversationId)).thenReturn(List.of(member));

        ConversationSettingsResponse response = conversationService.updateMute(conversationId, new UpdateConversationMuteRequest(userId, true));

        assertThat(response.getMembers()).hasSize(1);
        assertThat(response.getMembers().getFirst().isMuted()).isTrue();
    }

    @Test
    void updateNicknameShouldRejectNonOwnerEditingOthers() {
        UUID conversationId = UUID.randomUUID();
        UUID actorId = UUID.randomUUID();
        UUID targetId = UUID.randomUUID();

        Conversation conversation = conversation(conversationId, ConversationType.GROUP);
        ConversationMember actor = member(conversation, actorId, ConversationRole.MEMBER);
        ConversationMember target = member(conversation, targetId, ConversationRole.MEMBER);

        when(conversationMemberRepository.findByConversationIdAndUserId(conversationId, actorId)).thenReturn(Optional.of(actor));
        when(conversationMemberRepository.findByConversationIdAndUserId(conversationId, targetId)).thenReturn(Optional.of(target));

        UpdateConversationNicknameRequest request = new UpdateConversationNicknameRequest(actorId, "Best Friend");

        assertThatThrownBy(() -> conversationService.updateMemberNickname(conversationId, targetId, request))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN));
    }

    private Conversation conversation(UUID id, ConversationType type) {
        Conversation conversation = new Conversation();
        conversation.setId(id);
        conversation.setType(type);
        conversation.setName("Conversation");
        return conversation;
    }

    private ConversationMember member(Conversation conversation, UUID userId, ConversationRole role) {
        UserAccount user = new UserAccount();
        user.setId(userId);
        user.setUsername("u-" + userId);
        user.setDisplayName("Display " + userId);

        ConversationMember member = new ConversationMember();
        member.setConversation(conversation);
        member.setUser(user);
        member.setRole(role);
        return member;
    }
}

