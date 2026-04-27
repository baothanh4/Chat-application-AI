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
public class UpdateConversationSettingsRequest {
    @NotNull
    private UUID userId;

    @Size(max = 120)
    private String name;

    @Size(max = 255)
    private String description;

    @Size(max = 500)
    private String avatarPath;

    @Size(max = 32)
    private String themeColor;

    @Size(max = 16)
    private String quickReactionEmoji;

    private Boolean readReceiptEnabled;

    @Min(0)
    @Max(604800)
    private Integer disappearingMessagesSeconds;
}

