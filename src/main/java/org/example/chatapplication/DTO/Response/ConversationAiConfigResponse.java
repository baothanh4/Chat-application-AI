package org.example.chatapplication.DTO.Response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ConversationAiConfigResponse {
    private UUID conversationId;
    private UUID requestedByUserId;
    private String systemPrompt;
    private String behaviorPrompt;
    private Double temperature;
    private Integer maxTokens;
    private boolean useOfflineModel;
    private Instant updatedAt;
}

