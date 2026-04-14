package org.example.chatapplication.Controller;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.example.chatapplication.DTO.Request.MarkConversationReadRequest;
import org.example.chatapplication.DTO.Response.ConversationMessagesResponse;
import org.example.chatapplication.Service.ChatMessageService;
import org.example.chatapplication.Service.ConversationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConversationControllerTest {
    @Mock
    private ConversationService conversationService;

    @Mock
    private ChatMessageService chatMessageService;

    private ConversationController controller;
    private Validator validator;

    @BeforeEach
    void setUp() {
        controller = new ConversationController(conversationService, chatMessageService);
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            validator = factory.getValidator();
        }
    }

    @Test
    void markConversationReadShouldReturnConversationMessages() {
        UUID conversationId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        ConversationMessagesResponse response = new ConversationMessagesResponse(conversationId, null, List.of());
        when(chatMessageService.getConversationMessages(conversationId)).thenReturn(response);

        ConversationMessagesResponse actual = controller.markConversationRead(conversationId, new MarkConversationReadRequest(userId)).getBody();

        assertThat(actual).isNotNull();
        assertThat(actual.getConversationId()).isEqualTo(conversationId);
        assertThat(actual.getMessages()).isEmpty();

        verify(chatMessageService).markRead(eq(conversationId), eq(userId));
        verify(chatMessageService).getConversationMessages(eq(conversationId));
    }

    @Test
    void markConversationReadRequestShouldRequireUserId() {
        Set<jakarta.validation.ConstraintViolation<MarkConversationReadRequest>> violations = validator.validate(new MarkConversationReadRequest(null));

        assertThat(violations).isNotEmpty();
        assertThat(violations.iterator().next().getMessage()).contains("must not be null");
    }
}
