package org.example.chatapplication.Controller;

import lombok.RequiredArgsConstructor;
import org.example.chatapplication.DTO.Response.ConversationAiInsightResponse;
import org.example.chatapplication.DTO.Response.ConversationAiTaskResponse;
import org.example.chatapplication.Service.ConversationAiService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class AiInsightController {
    private final ConversationAiService conversationAiService;

    @GetMapping("/conversations/{conversationId}")
    public ResponseEntity<ConversationAiInsightResponse> getConversationInsight(@PathVariable UUID conversationId) {
        return ResponseEntity.ok(conversationAiService.getConversationInsight(conversationId));
    }

    @PostMapping("/conversations/{conversationId}/refresh")
    public ResponseEntity<ConversationAiInsightResponse> refreshConversationInsight(@PathVariable UUID conversationId) {
        return ResponseEntity.status(HttpStatus.CREATED).body(conversationAiService.refreshConversationInsight(conversationId));
    }

    @GetMapping("/conversations/{conversationId}/tasks")
    public ResponseEntity<List<ConversationAiTaskResponse>> listConversationTasks(@PathVariable UUID conversationId) {
        return ResponseEntity.ok(conversationAiService.listTasksForConversation(conversationId));
    }

    @GetMapping("/users/{userId}/tasks")
    public ResponseEntity<List<ConversationAiTaskResponse>> listUserTasks(@PathVariable UUID userId) {
        return ResponseEntity.ok(conversationAiService.listTasksForUser(userId));
    }
}

