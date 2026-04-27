package org.example.chatapplication.DTO.Response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.chatapplication.Model.Enum.MessageStatus;
import org.example.chatapplication.Model.Enum.MessageType;


import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ChatMessageResponse {
    UUID id;
    UUID conversationId;
    UserResponse sender;
    MessageType messageType;
    MessageStatus status;
    String content;
    String clientMessageId;
    UUID replyToMessageId;
    boolean edited;
    Instant editedAt;
    boolean unsent;
    Instant unsentAt;
    boolean deletedForEveryone;
    Instant deletedAt;
    UUID deletedByUserId;
    boolean pinned;
    Instant pinnedAt;
    UUID pinnedByUserId;
    List<MessageReactionSummaryResponse> reactions;
    Instant createdAt;
    Instant deliveredAt;
    Instant readAt;
}
