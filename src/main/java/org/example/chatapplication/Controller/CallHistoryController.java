package org.example.chatapplication.Controller;

import lombok.RequiredArgsConstructor;
import org.example.chatapplication.DTO.Response.CallHistoryResponse;
import org.example.chatapplication.Service.CallHistoryService;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/calls")
@RequiredArgsConstructor
public class CallHistoryController {
    private final CallHistoryService callHistoryService;

    @GetMapping("/users/{userId}")
    ResponseEntity<Page<CallHistoryResponse>> listByUser(@PathVariable UUID userId,
                                                         @RequestParam(defaultValue = "0") int page,
                                                         @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(callHistoryService.listByUser(userId, page, size));
    }

    @GetMapping("/conversations/{conversationId}")
    ResponseEntity<Page<CallHistoryResponse>> listByConversation(@PathVariable UUID conversationId,
                                                                 @RequestParam(defaultValue = "0") int page,
                                                                 @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(callHistoryService.listByConversation(conversationId, page, size));
    }
}

