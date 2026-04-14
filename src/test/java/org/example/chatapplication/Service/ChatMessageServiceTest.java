package org.example.chatapplication.Service;

import org.example.chatapplication.Model.Entity.ChatMessage;
import org.example.chatapplication.Model.Entity.Conversation;
import org.example.chatapplication.Model.Entity.ConversationMember;
import org.example.chatapplication.Model.Entity.UserAccount;
import org.example.chatapplication.Model.Enum.ConversationRole;
import org.example.chatapplication.Model.Enum.ConversationType;
import org.example.chatapplication.Model.Enum.MessageStatus;
import org.example.chatapplication.Repository.ChatMessageRepository;
import org.example.chatapplication.Repository.ConversationMemberRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChatMessageServiceTest {
    @Mock
    private ChatMessageRepository chatMessageRepository;
    @Mock
    private ConversationService conversationService;
    @Mock
    private UserAccountService userAccountService;
    @Mock
    private MessageQueueService messageQueueService;
    @Mock
    private ConversationMemberRepository conversationMemberRepository;

    private ChatMessageService chatMessageService;

    @BeforeEach
    void setUp() {
        chatMessageService = new ChatMessageService(
                chatMessageRepository,
                conversationService,
                userAccountService,
                messageQueueService,
                conversationMemberRepository
        );
    }

    @Test
    void markReadUpdatesMemberTimestampAndLatestMessageState() {
        UUID conversationId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        Conversation conversation = new Conversation();
        conversation.setId(conversationId);
        conversation.setType(ConversationType.PRIVATE);
        conversation.setName("private-chat");

        UserAccount user = new UserAccount();
        user.setId(userId);
        user.setUsername("alice");
        user.setDisplayName("Alice");

        ConversationMember member = new ConversationMember();
        member.setConversation(conversation);
        member.setUser(user);
        member.setRole(ConversationRole.MEMBER);

        ChatMessage latestMessage = new ChatMessage();
        latestMessage.setId(UUID.randomUUID());
        latestMessage.setConversation(conversation);
        latestMessage.setSender(user);
        latestMessage.setContent("hello");
        latestMessage.setStatus(MessageStatus.SENT);
        latestMessage.setCreatedAt(Instant.parse("2026-04-12T18:00:00Z"));

        when(conversationMemberRepository.findByConversationIdAndUserId(conversationId, userId)).thenReturn(Optional.of(member));
        when(conversationMemberRepository.save(any(ConversationMember.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(chatMessageRepository.findByConversationIdOrderByCreatedAtDesc(eq(conversationId), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of(latestMessage)));
        when(chatMessageRepository.save(any(ChatMessage.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ChatMessage result = chatMessageService.markRead(conversationId, userId);

        ArgumentCaptor<ConversationMember> memberCaptor = ArgumentCaptor.forClass(ConversationMember.class);
        verify(conversationMemberRepository).save(memberCaptor.capture());
        assertThat(memberCaptor.getValue().getLastReadAt()).isNotNull();

        ArgumentCaptor<ChatMessage> messageCaptor = ArgumentCaptor.forClass(ChatMessage.class);
        verify(chatMessageRepository).save(messageCaptor.capture());
        assertThat(messageCaptor.getValue().getStatus()).isEqualTo(MessageStatus.READ);
        assertThat(messageCaptor.getValue().getReadAt()).isNotNull();
        assertThat(result.getStatus()).isEqualTo(MessageStatus.READ);
        assertThat(result.getReadAt()).isNotNull();
    }

    @Test
    void markReadThrowsWhenConversationMemberIsMissing() {
        UUID conversationId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        when(conversationMemberRepository.findByConversationIdAndUserId(conversationId, userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> chatMessageService.markRead(conversationId, userId))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Conversation member not found");
    }

    @Test
    void markReadThrowsWhenConversationHasNoMessages() {
        UUID conversationId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        ConversationMember member = new ConversationMember();
        member.setLastReadAt(null);

        when(conversationMemberRepository.findByConversationIdAndUserId(conversationId, userId)).thenReturn(Optional.of(member));
        when(conversationMemberRepository.save(any(ConversationMember.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(chatMessageRepository.findByConversationIdOrderByCreatedAtDesc(eq(conversationId), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of()));

        assertThatThrownBy(() -> chatMessageService.markRead(conversationId, userId))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("No message found for conversation");
    }
}
