package org.example.chatapplication.Service;

import lombok.RequiredArgsConstructor;
import org.example.chatapplication.DTO.Request.CallSignalRequest;
import org.example.chatapplication.DTO.Response.CallHistoryResponse;
import org.example.chatapplication.Model.Entity.CallHistory;
import org.example.chatapplication.Model.Entity.Conversation;
import org.example.chatapplication.Model.Entity.UserAccount;
import org.example.chatapplication.Model.Enum.CallMode;
import org.example.chatapplication.Model.Enum.CallSignalType;
import org.example.chatapplication.Model.Enum.CallStatus;
import org.example.chatapplication.Repository.CallHistoryRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Service
@RequiredArgsConstructor
public class CallHistoryService {
    private final CallHistoryRepository callHistoryRepository;
    private final ConversationService conversationService;
    private final UserAccountService userAccountService;
    private final ChatMessageService chatMessageService;

    private final ConcurrentMap<String, UUID> activeCallByPair = new ConcurrentHashMap<>();

    @Transactional
    public UUID captureSignal(CallSignalRequest request, CallMode mode) {
        if (request.getType() == CallSignalType.INVITE) {
            return createCall(request, mode);
        }

        UUID callId = resolveCallId(request);
        if (callId == null) {
            return null;
        }

        if (request.getType() == CallSignalType.ACCEPT) {
            markAnswered(callId);
            return callId;
        }

        if (request.getType() == CallSignalType.REJECT) {
            finalizeCall(callId, request, CallStatus.REJECTED);
            return callId;
        }

        if (request.getType() == CallSignalType.CANCEL) {
            finalizeCall(callId, request, CallStatus.MISSED);
            return callId;
        }

        if (request.getType() == CallSignalType.END) {
            finalizeCall(callId, request, CallStatus.ENDED);
            return callId;
        }

        return callId;
    }

    @Transactional(readOnly = true)
    public Page<CallHistoryResponse> listByConversation(UUID conversationId, int page, int size) {
        conversationService.requireConversation(conversationId);
        return callHistoryRepository
                .findByConversationIdOrderByStartedAtDesc(conversationId, PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "startedAt")))
                .map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public Page<CallHistoryResponse> listByUser(UUID userId, int page, int size) {
        userAccountService.requireUser(userId);
        return callHistoryRepository
                .findByUserId(userId, PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "startedAt")))
                .map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public CallHistoryResponse toResponse(CallHistory history) {
        return new CallHistoryResponse(
                history.getId(),
                history.getConversation().getId(),
                userAccountService.toResponse(history.getCaller()),
                userAccountService.toResponse(history.getCallee()),
                history.getMode(),
                history.getStatus(),
                history.getStartedAt(),
                history.getAnsweredAt(),
                history.getEndedAt(),
                history.getDurationSeconds() == null ? 0L : history.getDurationSeconds(),
                history.getEndedByUserId()
        );
    }

    private UUID createCall(CallSignalRequest request, CallMode mode) {
        Conversation conversation = conversationService.requireConversation(request.getConversationId());
        UserAccount caller = userAccountService.requireUser(request.getFromUserId());
        UserAccount callee = userAccountService.requireUser(request.getToUserId());

        CallHistory call = new CallHistory();
        call.setConversation(conversation);
        call.setCaller(caller);
        call.setCallee(callee);
        call.setMode(mode);
        call.setStatus(CallStatus.RINGING);
        call.setStartedAt(Instant.now());

        CallHistory saved = callHistoryRepository.save(call);
        String pairKey = buildPairKey(request.getConversationId(), request.getFromUserId(), request.getToUserId());
        activeCallByPair.put(pairKey, saved.getId());
        return saved.getId();
    }

    private UUID resolveCallId(CallSignalRequest request) {
        if (request.getCallId() != null) {
            return request.getCallId();
        }

        String pairKey = buildPairKey(request.getConversationId(), request.getFromUserId(), request.getToUserId());
        return activeCallByPair.get(pairKey);
    }

    private void markAnswered(UUID callId) {
        CallHistory call = requireCall(callId);
        if (call.getAnsweredAt() == null) {
            call.setAnsweredAt(Instant.now());
            call.setStatus(CallStatus.ANSWERED);
            callHistoryRepository.save(call);
        }
    }

    private void finalizeCall(UUID callId, CallSignalRequest request, CallStatus finalStatus) {
        CallHistory call = requireCall(callId);
        if (call.getEndedAt() != null) {
            return;
        }

        Instant endTime = Instant.now();
        call.setEndedAt(endTime);
        call.setEndedByUserId(request.getFromUserId());

        CallStatus computedStatus = finalStatus;
        if (finalStatus == CallStatus.ENDED && call.getAnsweredAt() == null) {
            computedStatus = CallStatus.MISSED;
        }
        call.setStatus(computedStatus);

        long durationSeconds = 0L;
        if (call.getAnsweredAt() != null) {
            durationSeconds = Math.max(0L, Duration.between(call.getAnsweredAt(), endTime).getSeconds());
        }
        call.setDurationSeconds(durationSeconds);
        callHistoryRepository.save(call);

        String pairKey = buildPairKey(call.getConversation().getId(), call.getCaller().getId(), call.getCallee().getId());
        activeCallByPair.remove(pairKey, call.getId());

        chatMessageService.sendSystemMessage(
                call.getConversation().getId(),
                request.getFromUserId(),
                buildCallLogMessage(call)
        );
    }

    private String buildCallLogMessage(CallHistory call) {
        long duration = call.getDurationSeconds() == null ? 0L : call.getDurationSeconds();
        return "CALL_LOG|"
                + call.getId() + "|"
                + call.getMode() + "|"
                + call.getStatus() + "|"
                + duration + "|"
                + call.getCaller().getId() + "|"
                + call.getCallee().getId();
    }

    private CallHistory requireCall(UUID callId) {
        return callHistoryRepository.findById(callId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Call not found: " + callId));
    }

    private String buildPairKey(UUID conversationId, UUID firstUserId, UUID secondUserId) {
        String a = firstUserId.toString();
        String b = secondUserId.toString();
        if (a.compareTo(b) > 0) {
            String tmp = a;
            a = b;
            b = tmp;
        }
        return conversationId + ":" + a + ":" + b;
    }
}

