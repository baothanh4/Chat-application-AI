package org.example.chatapplication.Service;

import org.example.chatapplication.DTO.Request.ReplyMessageRequest;
import org.example.chatapplication.Model.Entity.ChatMessage;
import org.example.chatapplication.Model.Entity.Conversation;
import org.example.chatapplication.Model.Entity.ConversationMember;
import org.example.chatapplication.Model.Entity.MessageReaction;
import org.example.chatapplication.Model.Entity.UserAccount;
import org.example.chatapplication.Model.Enum.ConversationRole;
import org.example.chatapplication.Model.Enum.ConversationType;
import org.example.chatapplication.Model.Enum.MessageStatus;
import org.example.chatapplication.Model.Enum.MessageType;
import org.example.chatapplication.Repository.MessageReactionRepository;
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
import org.springframework.http.HttpStatus;
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
    @Mock
    private MessageReactionRepository messageReactionRepository;

    private ChatMessageService chatMessageService;

    @BeforeEach
    void setUp() {
        chatMessageService = new ChatMessageService(
                chatMessageRepository,
                conversationService,
                userAccountService,
                messageQueueService,
                conversationMemberRepository,
                messageReactionRepository
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

    @Test
    void pinMessageShouldMarkMessageAsPinned() {
        UUID messageId = UUID.randomUUID();
        UUID actorId = UUID.randomUUID();
        UUID conversationId = UUID.randomUUID();

        Conversation conversation = new Conversation();
        conversation.setId(conversationId);

        UserAccount sender = new UserAccount();
        sender.setId(UUID.randomUUID());

        ChatMessage message = new ChatMessage();
        message.setId(messageId);
        message.setConversation(conversation);
        message.setSender(sender);
        message.setContent("hello");
        message.setStatus(MessageStatus.SENT);

        when(chatMessageRepository.findById(messageId)).thenReturn(Optional.of(message));
        when(conversationService.isMember(conversationId, actorId)).thenReturn(true);
        when(chatMessageRepository.save(any(ChatMessage.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(userAccountService.toResponse(any(UserAccount.class))).thenReturn(null);
        when(messageReactionRepository.findByMessageIdOrderByCreatedAtAsc(messageId)).thenReturn(List.of());

        var response = chatMessageService.pinMessage(messageId, actorId);

        assertThat(response.isPinned()).isTrue();
        assertThat(response.getPinnedByUserId()).isEqualTo(actorId);
        assertThat(response.getPinnedAt()).isNotNull();
    }

    @Test
    void addOrUpdateReactionShouldAggregateEmojiCounts() {
        UUID messageId = UUID.randomUUID();
        UUID actorId = UUID.randomUUID();
        UUID otherUserId = UUID.randomUUID();
        UUID conversationId = UUID.randomUUID();

        Conversation conversation = new Conversation();
        conversation.setId(conversationId);

        UserAccount sender = new UserAccount();
        sender.setId(UUID.randomUUID());

        UserAccount actor = new UserAccount();
        actor.setId(actorId);

        ChatMessage message = new ChatMessage();
        message.setId(messageId);
        message.setConversation(conversation);
        message.setSender(sender);
        message.setContent("hello");
        message.setStatus(MessageStatus.SENT);

        MessageReaction actorReaction = new MessageReaction();
        actorReaction.setMessage(message);
        actorReaction.setUser(actor);
        actorReaction.setEmoji("🔥");

        UserAccount otherUser = new UserAccount();
        otherUser.setId(otherUserId);
        MessageReaction otherReaction = new MessageReaction();
        otherReaction.setMessage(message);
        otherReaction.setUser(otherUser);
        otherReaction.setEmoji("🔥");

        when(chatMessageRepository.findById(messageId)).thenReturn(Optional.of(message));
        when(conversationService.isMember(conversationId, actorId)).thenReturn(true);
        when(userAccountService.requireUser(actorId)).thenReturn(actor);
        when(messageReactionRepository.findByMessageIdAndUserId(messageId, actorId)).thenReturn(Optional.of(actorReaction));
        when(messageReactionRepository.save(any(MessageReaction.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(messageReactionRepository.findByMessageIdOrderByCreatedAtAsc(messageId)).thenReturn(List.of(actorReaction, otherReaction));
        when(userAccountService.toResponse(any(UserAccount.class))).thenReturn(null);

        var response = chatMessageService.addOrUpdateReaction(messageId, actorId, "🔥");

        assertThat(response.getReactions()).hasSize(1);
        assertThat(response.getReactions().getFirst().getEmoji()).isEqualTo("🔥");
        assertThat(response.getReactions().getFirst().getCount()).isEqualTo(2);
    }

    @Test
    void replyMessageShouldSetReplyToMessageId() {
        UUID conversationId = UUID.randomUUID();
        UUID targetMessageId = UUID.randomUUID();
        UUID actorId = UUID.randomUUID();

        Conversation conversation = new Conversation();
        conversation.setId(conversationId);

        UserAccount sender = new UserAccount();
        sender.setId(actorId);

        ChatMessage target = new ChatMessage();
        target.setId(targetMessageId);
        target.setConversation(conversation);
        target.setSender(sender);
        target.setContent("parent");

        when(chatMessageRepository.findById(targetMessageId)).thenReturn(Optional.of(target));
        when(conversationService.isMember(conversationId, actorId)).thenReturn(true);
        when(userAccountService.requireUser(actorId)).thenReturn(sender);
        when(chatMessageRepository.save(any(ChatMessage.class))).thenAnswer(invocation -> {
            ChatMessage saved = invocation.getArgument(0);
            saved.setId(UUID.randomUUID());
            return saved;
        });
        when(userAccountService.toResponse(any(UserAccount.class))).thenReturn(null);
        when(messageReactionRepository.findByMessageIdOrderByCreatedAtAsc(any())).thenReturn(List.of());

        var response = chatMessageService.replyMessage(targetMessageId, new ReplyMessageRequest(actorId, "reply", "c1"));

        assertThat(response.getReplyToMessageId()).isEqualTo(targetMessageId);
        assertThat(response.getConversationId()).isEqualTo(conversationId);
    }

    @Test
    void editMessageShouldRejectNonSender() {
        UUID conversationId = UUID.randomUUID();
        UUID messageId = UUID.randomUUID();
        UUID actorId = UUID.randomUUID();

        Conversation conversation = new Conversation();
        conversation.setId(conversationId);

        UserAccount sender = new UserAccount();
        sender.setId(UUID.randomUUID());

        ChatMessage message = new ChatMessage();
        message.setId(messageId);
        message.setConversation(conversation);
        message.setSender(sender);
        message.setContent("hello");

        when(chatMessageRepository.findById(messageId)).thenReturn(Optional.of(message));
        when(conversationService.isMember(conversationId, actorId)).thenReturn(true);

        assertThatThrownBy(() -> chatMessageService.editMessage(messageId, actorId, "updated"))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN));
    }

    @Test
    void unsendMessageShouldSetUnsentFlags() {
        UUID conversationId = UUID.randomUUID();
        UUID messageId = UUID.randomUUID();
        UUID actorId = UUID.randomUUID();

        Conversation conversation = new Conversation();
        conversation.setId(conversationId);

        UserAccount sender = new UserAccount();
        sender.setId(actorId);

        ChatMessage message = new ChatMessage();
        message.setId(messageId);
        message.setConversation(conversation);
        message.setSender(sender);
        message.setContent("hello");

        when(chatMessageRepository.findById(messageId)).thenReturn(Optional.of(message));
        when(conversationService.isMember(conversationId, actorId)).thenReturn(true);
        when(chatMessageRepository.save(any(ChatMessage.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(userAccountService.toResponse(any(UserAccount.class))).thenReturn(null);
        when(messageReactionRepository.findByMessageIdOrderByCreatedAtAsc(messageId)).thenReturn(List.of());

        var response = chatMessageService.unsendMessage(messageId, actorId);

        assertThat(response.isUnsent()).isTrue();
        assertThat(response.getUnsentAt()).isNotNull();
        assertThat(response.getContent()).isEqualTo("Message was unsent");
    }

    @Test
    void getConversationMediaShouldRejectUnsupportedType() {
        UUID conversationId = UUID.randomUUID();
        UUID actorId = UUID.randomUUID();

        when(conversationService.isMember(conversationId, actorId)).thenReturn(true);

        assertThatThrownBy(() -> chatMessageService.getConversationMedia(conversationId, actorId, MessageType.TEXT, 0, 10))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST));
    }
}
