package org.example.chatapplication.DTO.Response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PresenceResponse {
    UUID userId;
    boolean online;
    Instant lastSeenAt;
    Long minutesSinceLastActive;
    String source;
}
