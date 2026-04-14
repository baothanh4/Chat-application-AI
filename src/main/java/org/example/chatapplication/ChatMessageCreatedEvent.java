package org.example.chatapplication;

import java.util.UUID;

public record ChatMessageCreatedEvent(UUID messageId, UUID conversationId, UUID senderId) {
}

