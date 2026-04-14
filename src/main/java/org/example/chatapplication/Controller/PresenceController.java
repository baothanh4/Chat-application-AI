package org.example.chatapplication.Controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.example.chatapplication.DTO.Request.PresenceHeartbeatRequest;
import org.example.chatapplication.DTO.Response.PresenceResponse;
import org.example.chatapplication.Service.PresenceService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/presence")
@RequiredArgsConstructor
public class PresenceController {
    private final PresenceService presenceService;

    @PostMapping("/heartbeat")
    ResponseEntity<PresenceResponse> heartbeat(@RequestBody @Valid PresenceHeartbeatRequest request) {
        presenceService.markOnline(request.getUserId());
        return ResponseEntity.ok(presenceService.getPresence(request.getUserId()));
    }

    @PostMapping("/{userId}/offline")
    ResponseEntity<PresenceResponse> offline(@PathVariable UUID userId) {
        presenceService.markOffline(userId);
        return ResponseEntity.ok(presenceService.getPresence(userId));
    }

    @GetMapping("/{userId}")
    ResponseEntity<PresenceResponse> getPresence(@PathVariable UUID userId) {
        return ResponseEntity.ok(presenceService.getPresence(userId));
    }
}
