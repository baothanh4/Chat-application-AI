package org.example.chatapplication.Controller;

import lombok.RequiredArgsConstructor;

import org.example.chatapplication.DTO.Request.CallSignalRequest;
import org.example.chatapplication.DTO.Request.SendMessageRequest;
import org.example.chatapplication.DTO.Response.CallSignalResponse;
import org.example.chatapplication.DTO.Response.ChatMessageResponse;
import org.example.chatapplication.Model.Enum.CallMode;
import org.example.chatapplication.Model.Enum.CallSignalType;
import org.example.chatapplication.Service.CallSignalingService;
import org.example.chatapplication.Service.ChatMessageService;
import org.example.chatapplication.Service.PresenceService;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.time.Instant;
import java.util.Map;

@Controller
@RequiredArgsConstructor
public class ChatSocketController {
    private final ChatMessageService chatMessageService;
    private final CallSignalingService callSignalingService;
    private final PresenceService presenceService;
    private final SimpMessagingTemplate messagingTemplate;

    @MessageMapping("/chat.send")
    public void sendMessage(SendMessageRequest request) {
        presenceService.markOnline(request.getSenderId());
        ChatMessageResponse response = chatMessageService.sendMessage(request);
        messagingTemplate.convertAndSend("/topic/conversations/" + response.getConversationId(), response);
    }

    @MessageMapping("/call.signal")
    public void callSignal(CallSignalRequest request) {
        try {
            CallSignalResponse response = callSignalingService.validateAndBuild(request);
            messagingTemplate.convertAndSend("/topic/users/" + response.getToUserId() + "/calls", response);
        } catch (Exception ex) {
            if (request == null || request.getFromUserId() == null) {
                return;
            }
            CallSignalResponse error = new CallSignalResponse(
                    request.getCallId(),
                    request.getConversationId(),
                    request.getFromUserId(),
                    request.getFromUserId(),
                    CallSignalType.ERROR,
                    request.getMode() == null ? CallMode.AUDIO : request.getMode(),
                    Map.of("message", ex.getMessage() == null ? "Call signaling failed" : ex.getMessage()),
                    Instant.now()
            );
            messagingTemplate.convertAndSend("/topic/users/" + request.getFromUserId() + "/calls", error);
        }
    }
}
