package org.example.chatapplication.DTO.Response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.chatapplication.Model.Enum.ConversationType;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ConversationInboxItemResponse {
    private UUID conversationId;
    private ConversationType conversationType;
    private String name;
    private String description;
    private boolean archived;
    private Set<UserResponse> members;
    private ChatMessageResponse latestMessage;
    private UserResponse latestSender;
    private Instant latestMessageAt;
    private long unreadCount;
    private Instant createdAt;
    private Instant updatedAt;
}

