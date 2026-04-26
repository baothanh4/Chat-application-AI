package org.example.chatapplication.DTO.Response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GlobalAiPolicyResponse {
    private UUID id;
    private boolean enabled;
    private String systemPrompt;
    private String prohibitedTopics;
    private Integer maxTokens;
    private Double temperature;
    private Instant updatedAt;
}

