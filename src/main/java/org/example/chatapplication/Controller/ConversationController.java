package org.example.chatapplication.Controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.chatapplication.DTO.Request.CreateGroupConversationRequest;
import org.example.chatapplication.DTO.Request.CreatePrivateConversationRequest;
import org.example.chatapplication.DTO.Request.MarkConversationReadRequest;
import org.example.chatapplication.DTO.Response.ConversationInboxItemResponse;
import org.example.chatapplication.DTO.Response.ConversationMessagesResponse;
import org.example.chatapplication.DTO.Response.ConversationResponse;
import org.example.chatapplication.DTO.Response.MessageHistoryResponse;
import org.example.chatapplication.Service.ChatMessageService;
import org.example.chatapplication.Service.ConversationService;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;


@RestController
@RequestMapping("/api/conversations")
@RequiredArgsConstructor
@Validated
public class ConversationController {
    private final ConversationService conversationService;
    private final ChatMessageService chatMessageService;

    @PostMapping("/private")
    ResponseEntity<ConversationResponse> createPrivateConversation(@RequestBody @Valid CreatePrivateConversationRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(conversationService.createPrivateConversation(request));
    }

    @PostMapping("/group")
    ResponseEntity<ConversationResponse> createGroupConversation(@RequestBody @Valid CreateGroupConversationRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(conversationService.createGroupConversation(request));
    }

    @GetMapping("/users/{userId}")
    ResponseEntity<Page<ConversationResponse>> listConversations(@PathVariable UUID userId,
                                                                 @RequestParam(defaultValue = "0") int page,
                                                                 @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(conversationService.listConversations(userId, page, size));
    }

    @GetMapping("/users/{userId}/inbox")
    ResponseEntity<List<Map<String, Object>>> listInbox(@PathVariable UUID userId) {
        return ResponseEntity.ok(conversationService.listInbox(userId));
    }

    @PostMapping("/{conversationId}/read")
    ResponseEntity<ConversationMessagesResponse> markConversationRead(@PathVariable UUID conversationId,
                                                                      @RequestBody @Valid MarkConversationReadRequest request) {
        chatMessageService.markRead(conversationId, request.getUserId());
        return ResponseEntity.ok(chatMessageService.getConversationMessages(conversationId));
    }

    @GetMapping("/{conversationId}/messages")
    ResponseEntity<ConversationMessagesResponse> getConversationMessages(@PathVariable UUID conversationId) {
        return ResponseEntity.ok(chatMessageService.getConversationMessages(conversationId));
    }

    @GetMapping("/{conversationId}/messages/page")
    ResponseEntity<MessageHistoryResponse> getHistory(@PathVariable UUID conversationId,
                                                      @RequestParam(defaultValue = "0") int page,
                                                      @RequestParam(defaultValue = "50") int size) {
        return ResponseEntity.ok(chatMessageService.getHistory(conversationId, page, size));
    }
}
