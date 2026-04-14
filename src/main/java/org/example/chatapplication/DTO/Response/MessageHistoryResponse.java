package org.example.chatapplication.DTO.Response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


import java.util.List;
import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class MessageHistoryResponse {
    UUID conversationId;
    ConversationResponse conversation;
    List<ChatMessageResponse> messages;
    long page;
    long size;
    long totalElements;
    long totalPages;
}
