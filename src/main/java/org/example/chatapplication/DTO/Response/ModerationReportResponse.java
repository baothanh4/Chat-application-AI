package org.example.chatapplication.DTO.Response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.chatapplication.Model.Enum.ModerationReportStatus;

import java.time.Instant;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ModerationReportResponse {
    private UUID id;
    private UUID reporterId;
    private String reporterUsername;
    private UUID targetUserId;
    private UUID conversationId;
    private UUID messageId;
    private String reason;
    private String details;
    private ModerationReportStatus status;
    private UUID reviewedById;
    private String reviewedByUsername;
    private Instant reviewedAt;
    private String moderatorNote;
    private Instant createdAt;
}

