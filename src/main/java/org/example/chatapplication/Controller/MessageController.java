package org.example.chatapplication.Controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.example.chatapplication.DTO.Request.SendMessageRequest;
import org.example.chatapplication.DTO.Response.ChatMessageResponse;
import org.example.chatapplication.Service.ChatMessageService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/messages")
@RequiredArgsConstructor
@Validated
public class MessageController {
    private final ChatMessageService chatMessageService;

    @PostMapping
    ResponseEntity<ChatMessageResponse> sendMessage(@RequestBody @Valid SendMessageRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(chatMessageService.sendMessage(request));
    }
}
