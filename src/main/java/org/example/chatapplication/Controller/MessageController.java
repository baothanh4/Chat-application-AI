package org.example.chatapplication.Controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.example.chatapplication.DTO.Request.SendMessageRequest;
import org.example.chatapplication.DTO.Response.ChatMessageResponse;
import org.example.chatapplication.Service.ChatMessageService;
import org.example.chatapplication.Service.FileStorageService;
import org.example.chatapplication.Model.Enum.MessageType;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@RestController
@RequestMapping("/api/messages")
@RequiredArgsConstructor
@Validated
public class MessageController {
    private final ChatMessageService chatMessageService;
    private final FileStorageService fileStorageService;

    @PostMapping
    ResponseEntity<ChatMessageResponse> sendMessage(@RequestBody @Valid SendMessageRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(chatMessageService.sendMessage(request));
    }

    @PostMapping(value = "/video", consumes = org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE)
    ResponseEntity<ChatMessageResponse> sendVideoMessage(@RequestParam(value = "conversationId", required = false) UUID conversationId,
                                                         @RequestParam("senderId") UUID senderId,
                                                         @RequestParam(value = "recipientId", required = false) UUID recipientId,
                                                         @RequestParam(value = "clientMessageId", required = false) String clientMessageId,
                                                         @RequestParam("file") MultipartFile file) {
        String videoUrl = fileStorageService.storeChatVideo(file);
        SendMessageRequest request = new SendMessageRequest();
        request.setConversationId(conversationId);
        request.setSenderId(senderId);
        request.setRecipientId(recipientId);
        request.setClientMessageId(clientMessageId);
        request.setMessageType(MessageType.VIDEO);
        request.setContent(videoUrl);
        return ResponseEntity.status(HttpStatus.CREATED).body(chatMessageService.sendMessage(request));
    }

    @PostMapping(value = "/image", consumes = org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE)
    ResponseEntity<ChatMessageResponse> sendImageMessage(@RequestParam(value = "conversationId", required = false) UUID conversationId,
                                                         @RequestParam("senderId") UUID senderId,
                                                         @RequestParam(value = "recipientId", required = false) UUID recipientId,
                                                         @RequestParam(value = "clientMessageId", required = false) String clientMessageId,
                                                         @RequestParam("file") MultipartFile file) {
        String imageUrl = fileStorageService.storeChatImage(file);
        SendMessageRequest request = new SendMessageRequest();
        request.setConversationId(conversationId);
        request.setSenderId(senderId);
        request.setRecipientId(recipientId);
        request.setClientMessageId(clientMessageId);
        request.setMessageType(MessageType.IMAGE);
        request.setContent(imageUrl);
        return ResponseEntity.status(HttpStatus.CREATED).body(chatMessageService.sendMessage(request));
    }
}
