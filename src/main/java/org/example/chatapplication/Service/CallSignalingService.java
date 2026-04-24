package org.example.chatapplication.Service;

import lombok.RequiredArgsConstructor;
import org.example.chatapplication.DTO.Request.CallSignalRequest;
import org.example.chatapplication.DTO.Response.CallSignalResponse;
import org.example.chatapplication.Model.Entity.Conversation;
import org.example.chatapplication.Model.Enum.CallMode;
import org.example.chatapplication.Model.Enum.CallSignalType;
import org.example.chatapplication.Model.Enum.ConversationType;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Service
@RequiredArgsConstructor
public class CallSignalingService {

    private final ConversationService conversationService;
    private final CallHistoryService callHistoryService;
    private final ConcurrentMap<UUID, UUID> activeCallPeers = new ConcurrentHashMap<>();

    @Transactional(readOnly = true)
    public CallSignalResponse validateAndBuild(CallSignalRequest request) {
        if (request == null || request.getConversationId() == null || request.getFromUserId() == null
                || request.getToUserId() == null || request.getType() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid call payload");
        }
        if (request.getFromUserId().equals(request.getToUserId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Caller and callee must be different users");
        }

        Conversation conversation = conversationService.requireConversation(request.getConversationId());
        if (conversation.getType() != ConversationType.PRIVATE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Calling is only available in private conversations");
        }
        if (!conversationService.isMember(request.getConversationId(), request.getFromUserId())
                || !conversationService.isMember(request.getConversationId(), request.getToUserId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Users are not members of this conversation");
        }

        CallMode mode = request.getMode() == null ? CallMode.AUDIO : request.getMode();
        Map<String, Object> payload = request.getPayload() == null ? Map.of() : request.getPayload();

        applyBusyState(request.getType(), request.getFromUserId(), request.getToUserId());
        UUID callId = callHistoryService.captureSignal(request, mode);

        return new CallSignalResponse(
                callId,
                request.getConversationId(),
                request.getFromUserId(),
                request.getToUserId(),
                request.getType(),
                mode,
                payload,
                Instant.now()
        );
    }

    private void applyBusyState(CallSignalType type, UUID fromUserId, UUID toUserId) {
        if (type == CallSignalType.INVITE) {
            UUID fromPeer = activeCallPeers.get(fromUserId);
            UUID toPeer = activeCallPeers.get(toUserId);
            if ((fromPeer != null && !fromPeer.equals(toUserId)) || (toPeer != null && !toPeer.equals(fromUserId))) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "One user is currently in another call");
            }
            return;
        }

        if (type == CallSignalType.ACCEPT) {
            activeCallPeers.put(fromUserId, toUserId);
            activeCallPeers.put(toUserId, fromUserId);
            return;
        }

        if (type == CallSignalType.REJECT || type == CallSignalType.CANCEL || type == CallSignalType.END) {
            clearBusyIfPaired(fromUserId, toUserId);
            clearBusyIfPaired(toUserId, fromUserId);
        }
    }

    private void clearBusyIfPaired(UUID userId, UUID expectedPeerId) {
        UUID currentPeer = activeCallPeers.get(userId);
        if (expectedPeerId.equals(currentPeer)) {
            activeCallPeers.remove(userId, currentPeer);
        }
    }
}

