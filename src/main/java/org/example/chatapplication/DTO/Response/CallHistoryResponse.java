package org.example.chatapplication.DTO.Response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.chatapplication.Model.Enum.CallMode;
import org.example.chatapplication.Model.Enum.CallStatus;

import java.time.Instant;
import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CallHistoryResponse {
    private UUID callId;
    private UUID conversationId;
    private UserResponse caller;
    private UserResponse callee;
    private CallMode mode;
    private CallStatus status;
    private Instant startedAt;
    private Instant answeredAt;
    private Instant endedAt;
    private long durationSeconds;
    private UUID endedByUserId;
}

