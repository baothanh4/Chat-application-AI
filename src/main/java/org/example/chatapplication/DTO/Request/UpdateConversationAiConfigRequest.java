package org.example.chatapplication.DTO.Request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UpdateConversationAiConfigRequest {
    @NotNull
    private UUID userId;

    @Size(max = 4000)
    private String systemPrompt;

    @Size(max = 2000)
    private String behaviorPrompt;

    @Min(0)
    @Max(2)
    private Double temperature;

    @Min(64)
    @Max(2048)
    private Integer maxTokens;

    private Boolean useOfflineModel;
}

