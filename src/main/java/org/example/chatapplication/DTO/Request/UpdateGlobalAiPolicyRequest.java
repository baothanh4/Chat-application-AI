package org.example.chatapplication.DTO.Request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UpdateGlobalAiPolicyRequest {

    private Boolean enabled;

    @Size(max = 4000)
    private String systemPrompt;

    @Size(max = 2000)
    private String prohibitedTopics;

    @Min(64)
    @Max(4096)
    private Integer maxTokens;

    @DecimalMin("0.0")
    @DecimalMax("2.0")
    private Double temperature;
}

