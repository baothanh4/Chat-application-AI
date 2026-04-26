package org.example.chatapplication.DTO.Response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.chatapplication.Model.Enum.UserRole;

import java.time.Instant;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AdminUserSummaryResponse {
    private UUID userId;
    private String username;
    private String displayName;
    private UserRole role;
    private boolean accountLocked;
    private Instant lastSeenAt;
    private Instant createdAt;
}

