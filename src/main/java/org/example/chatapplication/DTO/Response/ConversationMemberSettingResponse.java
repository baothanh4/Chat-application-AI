package org.example.chatapplication.DTO.Response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ConversationMemberSettingResponse {
    private UUID userId;
    private String username;
    private String displayName;
    private String nickname;
    private boolean muted;
    private Instant lastReadAt;
}

