package org.example.chatapplication.DTO.Response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ConversationMessagesResponse {
    UUID conversationId;
    ConversationResponse conversation;
    List<ChatMessageResponse> messages;
}

