package org.example.chatapplication.DTO.Response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ConversationMessageEventResponse {
    private String eventType;
    private UUID conversationId;
    private UUID messageId;
    private UUID actorUserId;
    private Instant occurredAt;
    private ChatMessageResponse message;
}

