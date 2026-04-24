package org.example.chatapplication.DTO.Request;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.example.chatapplication.Model.Enum.CallMode;
import org.example.chatapplication.Model.Enum.CallSignalType;

import java.util.Map;
import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class CallSignalRequest {
    private UUID callId;
    private UUID conversationId;
    private UUID fromUserId;
    private UUID toUserId;
    private CallSignalType type;
    private CallMode mode;
    private Map<String, Object> payload;
}

