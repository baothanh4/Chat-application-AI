package org.example.chatapplication.DTO.Request;

import jakarta.validation.constraints.NotBlank;
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
public class CreateModerationReportRequest {

    private UUID messageId;

    private UUID conversationId;

    private UUID targetUserId;

    @NotBlank
    @Size(max = 200)
    private String reason;

    @Size(max = 2000)
    private String details;
}

