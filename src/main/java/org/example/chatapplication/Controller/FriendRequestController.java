package org.example.chatapplication.Controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.chatapplication.DTO.Request.RespondFriendRequestRequest;
import org.example.chatapplication.DTO.Request.SendFriendRequestRequest;
import org.example.chatapplication.DTO.Response.FriendRequestResponse;
import org.example.chatapplication.DTO.Response.UserSearchResponse;
import org.example.chatapplication.Service.FriendRequestService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/friend-requests")
@RequiredArgsConstructor
@Validated
public class FriendRequestController {
    private final FriendRequestService friendRequestService;

    @PostMapping
    public ResponseEntity<FriendRequestResponse> sendFriendRequest(@RequestBody @Valid SendFriendRequestRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(friendRequestService.sendRequest(request));
    }

    @GetMapping("/incoming/{userId}")
    public ResponseEntity<List<FriendRequestResponse>> getIncomingRequests(@PathVariable UUID userId) {
        return ResponseEntity.ok(friendRequestService.getIncomingRequests(userId));
    }

    @GetMapping("/outgoing/{userId}")
    public ResponseEntity<List<FriendRequestResponse>> getOutgoingRequests(@PathVariable UUID userId) {
        return ResponseEntity.ok(friendRequestService.getOutgoingRequests(userId));
    }

    @GetMapping("/friends/{userId}")
    public ResponseEntity<List<UserSearchResponse>> getFriends(@PathVariable UUID userId) {
        return ResponseEntity.ok(friendRequestService.getFriends(userId));
    }

    @PostMapping("/{requestId}/accept")
    public ResponseEntity<FriendRequestResponse> acceptFriendRequest(@PathVariable UUID requestId,
                                                                     @RequestBody @Valid RespondFriendRequestRequest request) {
        return ResponseEntity.ok(friendRequestService.acceptRequest(requestId, request));
    }

    @PostMapping("/{requestId}/reject")
    public ResponseEntity<FriendRequestResponse> rejectFriendRequest(@PathVariable UUID requestId,
                                                                     @RequestBody @Valid RespondFriendRequestRequest request) {
        return ResponseEntity.ok(friendRequestService.rejectRequest(requestId, request));
    }
}

