package org.example.chatapplication.DTO.Response;

import org.example.chatapplication.Model.Enum.FriendRequestStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class FriendRequestResponse {
    private UUID id;
    private UserSearchResponse sender;
    private UserSearchResponse recipient;
    private FriendRequestStatus status;
    private String message;
    private Instant createdAt;
    private Instant updatedAt;
    private Instant respondedAt;
    private UUID privateConversationId;
}

