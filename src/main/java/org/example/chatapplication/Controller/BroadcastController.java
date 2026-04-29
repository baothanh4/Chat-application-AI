package org.example.chatapplication.Controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.chatapplication.DTO.Request.CreateBroadcastRequest;
import org.example.chatapplication.DTO.Response.BroadcastResponse;
import org.example.chatapplication.Service.BroadcastService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/broadcasts")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class BroadcastController {

    private final BroadcastService broadcastService;

    @PostMapping
    ResponseEntity<BroadcastResponse> createBroadcast(@RequestBody @Valid CreateBroadcastRequest request,
                                                     Authentication authentication) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(broadcastService.createBroadcast(request, authentication.getName()));
    }

    @GetMapping
    ResponseEntity<List<BroadcastResponse>> listBroadcasts() {
        return ResponseEntity.ok(broadcastService.listBroadcasts());
    }

    @GetMapping("/{broadcastId}")
    ResponseEntity<BroadcastResponse> getBroadcast(@PathVariable UUID broadcastId) {
        return ResponseEntity.ok(broadcastService.getBroadcast(broadcastId));
    }

    @DeleteMapping("/{broadcastId}")
    ResponseEntity<Void> deleteBroadcast(@PathVariable UUID broadcastId, Authentication authentication) {
        broadcastService.deleteBroadcast(broadcastId, authentication.getName());
        return ResponseEntity.noContent().build();
    }
}

