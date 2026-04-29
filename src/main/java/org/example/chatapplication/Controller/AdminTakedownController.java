package org.example.chatapplication.Controller;

import lombok.RequiredArgsConstructor;
import org.example.chatapplication.DTO.Response.ChatMessageResponse;
import org.example.chatapplication.Service.AdminTakedownService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/admin/takedown")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminTakedownController {

    private final AdminTakedownService adminTakedownService;

    @DeleteMapping("/messages/{messageId}")
    ResponseEntity<ChatMessageResponse> deleteMessage(@PathVariable UUID messageId,
                                                     @RequestParam(required = false) String reason,
                                                     Authentication authentication) {
        ChatMessageResponse response = adminTakedownService.adminDeleteMessage(messageId, authentication.getName(), reason);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/uploads")
    ResponseEntity<Void> deleteUpload(@RequestParam String filePath,
                                     @RequestParam(required = false) String reason,
                                     Authentication authentication) {
        adminTakedownService.adminRemoveFileUpload(filePath, authentication.getName(), reason);
        return ResponseEntity.noContent().build();
    }
}

