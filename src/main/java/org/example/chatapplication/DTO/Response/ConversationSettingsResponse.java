package org.example.chatapplication.DTO.Response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ConversationSettingsResponse {
    private UUID conversationId;
    private String name;
    private String description;
    private String avatarPath;
    private String themeColor;
    private String quickReactionEmoji;
    private boolean readReceiptEnabled;
    private int disappearingMessagesSeconds;
    private List<ConversationMemberSettingResponse> members;
    private Instant updatedAt;
}

