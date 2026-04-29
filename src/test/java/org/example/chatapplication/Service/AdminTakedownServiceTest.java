package org.example.chatapplication.Service;

import org.example.chatapplication.DTO.Response.ChatMessageResponse;
import org.example.chatapplication.Model.Entity.ChatMessage;
import org.example.chatapplication.Model.Entity.Conversation;
import org.example.chatapplication.Model.Entity.UserAccount;
import org.example.chatapplication.Model.Enum.AdminAuditAction;
import org.example.chatapplication.Model.Enum.MessageStatus;
import org.example.chatapplication.Model.Enum.MessageType;
import org.example.chatapplication.Repository.ChatMessageRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminTakedownServiceTest {

    @Mock
    private ChatMessageRepository chatMessageRepository;

    @Mock
    private AdminAuditService adminAuditService;

    @Mock
    private FileStorageService fileStorageService;

    @Mock
    private ChatMessageService chatMessageService;

    @InjectMocks
    private AdminTakedownService adminTakedownService;

    @Test
    void adminDeleteMessageShouldMarkAsDeletedForEveryone() {
        UUID messageId = UUID.randomUUID();
        UUID conversationId = UUID.randomUUID();

        UserAccount sender = mock(UserAccount.class);
        sender.setId(UUID.randomUUID());
        sender.setUsername("sender");
        sender.setDisplayName("Sender");

        Conversation conversation = mock(Conversation.class);
        conversation.setId(conversationId);

        ChatMessage message = new ChatMessage();
        message.setId(messageId);
        message.setSender(sender);
        message.setConversation(conversation);
        message.setContent("Test message");
        message.setMessageType(MessageType.TEXT);
        message.setStatus(MessageStatus.DELIVERED);
        message.setDeletedForEveryone(false);
        message.setDeletedAt(null);

        when(chatMessageRepository.findById(messageId)).thenReturn(Optional.of(message));
        when(chatMessageRepository.save(any(ChatMessage.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ChatMessageResponse response = adminTakedownService.adminDeleteMessage(messageId, "admin", "spam");

        assertThat(response.isDeletedForEveryone()).isTrue();
        assertThat(response.getDeletedAt()).isNotNull();
        verify(adminAuditService).log(eq(AdminAuditAction.MESSAGE_DELETED_BY_ADMIN), eq("admin"), eq(messageId), any());
    }

    @Test
    void adminDeleteMessageShouldFailIfNotFound() {
        UUID messageId = UUID.randomUUID();

        when(chatMessageRepository.findById(messageId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> adminTakedownService.adminDeleteMessage(messageId, "admin", "spam"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Message not found");
    }

    @Test
    void adminRemoveFileUploadShouldDeleteAndAudit() {
        String filePath = "/uploads/chat-images/test.jpg";

        adminTakedownService.adminRemoveFileUpload(filePath, "admin", "offense");

        verify(fileStorageService).deleteStoredFile(filePath);
        verify(adminAuditService).log(eq(AdminAuditAction.FILE_DELETED_BY_ADMIN), eq("admin"), eq(null), any());
    }

    @Test
    void adminRemoveFileUploadShouldThrowIfDeleteFails() {
        String filePath = "/uploads/chat-images/test.jpg";

        org.mockito.Mockito.doThrow(new RuntimeException("Disk error"))
                .when(fileStorageService).deleteStoredFile(filePath);

        assertThatThrownBy(() -> adminTakedownService.adminRemoveFileUpload(filePath, "admin", "offense"))
                .isInstanceOf(ResponseStatusException.class);
    }
}

