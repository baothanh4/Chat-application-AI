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
public class ConversationResponse {
    UUID id;
    ConversationType type;
    String name;
    String description;
    boolean archived;
    String avatarPath;
    Set<UserResponse> members;
    Instant createdAt;
    Instant updatedAt;
}
