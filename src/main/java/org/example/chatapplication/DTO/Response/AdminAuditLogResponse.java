package org.example.chatapplication.DTO.Response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.chatapplication.Model.Enum.AdminAuditAction;

import java.time.Instant;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AdminAuditLogResponse {
    private UUID id;
    private AdminAuditAction action;
    private String actorUsername;
    private UUID targetUserId;
    private String details;
    private Instant createdAt;
}

