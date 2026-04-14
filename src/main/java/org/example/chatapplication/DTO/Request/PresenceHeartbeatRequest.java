package org.example.chatapplication.DTO.Request;

import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class PresenceHeartbeatRequest {
    @NotNull
    UUID userId;
}
