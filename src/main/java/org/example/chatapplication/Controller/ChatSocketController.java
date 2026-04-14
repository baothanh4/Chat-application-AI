package org.example.chatapplication.Controller;

import lombok.RequiredArgsConstructor;

import org.example.chatapplication.DTO.Request.SendMessageRequest;
import org.example.chatapplication.DTO.Response.ChatMessageResponse;
import org.example.chatapplication.Service.ChatMessageService;
import org.example.chatapplication.Service.PresenceService;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

@Controller
@RequiredArgsConstructor
public class ChatSocketController {
    private final ChatMessageService chatMessageService;
    private final PresenceService presenceService;
    private final SimpMessagingTemplate messagingTemplate;

    @MessageMapping("/chat.send")
    public void sendMessage(SendMessageRequest request) {
        presenceService.markOnline(request.getSenderId());
        ChatMessageResponse response = chatMessageService.sendMessage(request);
        messagingTemplate.convertAndSend("/topic/conversations/" + response.getConversationId(), response);
    }
}
