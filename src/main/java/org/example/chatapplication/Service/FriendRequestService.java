package org.example.chatapplication.Service;

import lombok.RequiredArgsConstructor;
import org.example.chatapplication.DTO.Request.RespondFriendRequestRequest;
import org.example.chatapplication.DTO.Request.SendFriendRequestRequest;
import org.example.chatapplication.DTO.Response.FriendRequestResponse;
import org.example.chatapplication.DTO.Response.UserSearchResponse;
import org.example.chatapplication.Model.Entity.FriendRequest;
import org.example.chatapplication.Model.Entity.Friendship;
import org.example.chatapplication.Model.Entity.UserAccount;
import org.example.chatapplication.Model.Enum.FriendRequestStatus;
import org.example.chatapplication.Repository.FriendRequestRepository;
import org.example.chatapplication.Repository.FriendshipRepository;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FriendRequestService {
    private final FriendRequestRepository friendRequestRepository;
    private final FriendshipRepository friendshipRepository;
    private final UserDirectoryService userDirectoryService;
    private final ConversationService conversationService;
    private final PresenceService presenceService;
    private final SimpMessagingTemplate messagingTemplate;

    @Transactional
    public FriendRequestResponse sendRequest(SendFriendRequestRequest request) {
        UserAccount sender = userDirectoryService.requireUser(request.getSenderId());
        UserAccount recipient = userDirectoryService.requireUser(request.getRecipientId());
        validateDifferentUsers(sender.getId(), recipient.getId());
        ensureNotAlreadyFriends(sender.getId(), recipient.getId());
        ensureNoPendingRequest(sender.getId(), recipient.getId());

        FriendRequest friendRequest = new FriendRequest();
        friendRequest.setRequester(sender);
        friendRequest.setRecipient(recipient);
        friendRequest.setMessage(trimToNull(request.getMessage()));
        friendRequest.setStatus(FriendRequestStatus.PENDING);
        friendRequest = friendRequestRepository.save(friendRequest);

        FriendRequestResponse response = toResponse(friendRequest, sender.getId());
        publishFriendEvent(sender.getId(), recipient.getId(), response);
        return response;
    }

    @Transactional
    public FriendRequestResponse acceptRequest(UUID requestId, RespondFriendRequestRequest request) {
        FriendRequest friendRequest = requireRecipientRequest(requestId, request.getUserId());
        if (friendRequest.getStatus() != FriendRequestStatus.PENDING) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Friend request is no longer pending");
        }

        UserAccount sender = friendRequest.getRequester();
        UserAccount recipient = friendRequest.getRecipient();

        friendRequest.setStatus(FriendRequestStatus.ACCEPTED);
        friendRequest.setRespondedBy(recipient);
        friendRequest.setRespondedAt(Instant.now());

        UUID conversationId = conversationService.resolveOrCreatePrivateConversation(sender.getId(), recipient.getId()).getId();
        friendRequest.setPrivateConversationId(conversationId);
        friendRequestRepository.save(friendRequest);

        saveFriendship(sender.getId(), recipient.getId(), friendRequest.getId(), conversationId);

        FriendRequestResponse response = toResponse(friendRequest, recipient.getId());
        publishFriendEvent(sender.getId(), recipient.getId(), response);
        return response;
    }

    @Transactional
    public FriendRequestResponse rejectRequest(UUID requestId, RespondFriendRequestRequest request) {
        FriendRequest friendRequest = requireRecipientRequest(requestId, request.getUserId());
        if (friendRequest.getStatus() != FriendRequestStatus.PENDING) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Friend request is no longer pending");
        }

        friendRequest.setStatus(FriendRequestStatus.REJECTED);
        friendRequest.setRespondedBy(friendRequest.getRecipient());
        friendRequest.setRespondedAt(Instant.now());
        friendRequestRepository.save(friendRequest);

        FriendRequestResponse response = toResponse(friendRequest, request.getUserId());
        publishFriendEvent(friendRequest.getRequester().getId(), friendRequest.getRecipient().getId(), response);
        return response;
    }

    @Transactional(readOnly = true)
    public List<FriendRequestResponse> getIncomingRequests(UUID userId) {
        userDirectoryService.requireUser(userId);
        return friendRequestRepository.findByRecipientIdAndStatusOrderByCreatedAtDesc(userId, FriendRequestStatus.PENDING).stream()
                .map(request -> toResponse(request, userId))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<FriendRequestResponse> getOutgoingRequests(UUID userId) {
        userDirectoryService.requireUser(userId);
        return friendRequestRepository.findByRequesterIdAndStatusOrderByCreatedAtDesc(userId, FriendRequestStatus.PENDING).stream()
                .map(request -> toResponse(request, userId))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<UserSearchResponse> getFriends(UUID userId) {
        userDirectoryService.requireUser(userId);
        return friendshipRepository.findByUserLowIdOrUserHighIdOrderByCreatedAtDesc(userId, userId).stream()
                .map(friendship -> toFriendSummary(friendship, userId))
                .toList();
    }

    private FriendRequest requireRecipientRequest(UUID requestId, UUID recipientId) {
        FriendRequest friendRequest = friendRequestRepository.findByIdAndRecipientId(requestId, recipientId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Friend request not found: " + requestId));
        if (!recipientId.equals(friendRequest.getRecipient().getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only the recipient can respond to this friend request");
        }
        return friendRequest;
    }

    private void saveFriendship(UUID userA, UUID userB, UUID requestId, UUID conversationId) {
        UUID low = canonicalLow(userA, userB);
        UUID high = canonicalHigh(userA, userB);
        if (friendshipRepository.existsByUserLowIdAndUserHighId(low, high)) {
            return;
        }

        Friendship friendship = new Friendship();
        friendship.setUserLow(userDirectoryService.requireUser(low));
        friendship.setUserHigh(userDirectoryService.requireUser(high));
        friendship.setFriendRequestId(requestId);
        friendship.setPrivateConversationId(conversationId);
        friendshipRepository.save(friendship);
    }

    private void ensureNotAlreadyFriends(UUID userA, UUID userB) {
        UUID low = canonicalLow(userA, userB);
        UUID high = canonicalHigh(userA, userB);
        if (friendshipRepository.existsByUserLowIdAndUserHighId(low, high)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "You are already friends");
        }
    }

    private void ensureNoPendingRequest(UUID userA, UUID userB) {
        List<FriendRequest> existing = friendRequestRepository.findBetweenUsers(userA, userB);
        boolean pendingExists = existing.stream().anyMatch(request -> request.getStatus() == FriendRequestStatus.PENDING);
        if (pendingExists) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "A pending friend request already exists between these users");
        }
    }

    private FriendRequestResponse toResponse(FriendRequest friendRequest, UUID viewerId) {
        return new FriendRequestResponse(
                friendRequest.getId(),
                userDirectoryService.toSearchResponse(friendRequest.getRequester(), viewerId),
                userDirectoryService.toSearchResponse(friendRequest.getRecipient(), viewerId),
                friendRequest.getStatus(),
                friendRequest.getMessage(),
                friendRequest.getCreatedAt(),
                friendRequest.getUpdatedAt(),
                friendRequest.getRespondedAt(),
                friendRequest.getPrivateConversationId()
        );
    }

    private UserSearchResponse toFriendSummary(Friendship friendship, UUID viewerId) {
        UserAccount friend = viewerId.equals(friendship.getUserLow().getId()) ? friendship.getUserHigh() : friendship.getUserLow();
        return userDirectoryService.toSearchResponse(friend, viewerId);
    }

    private void publishFriendEvent(UUID requesterId, UUID recipientId, FriendRequestResponse response) {
        messagingTemplate.convertAndSend("/topic/friends/" + requesterId, response);
        messagingTemplate.convertAndSend("/topic/friends/" + recipientId, response);

        if (!presenceService.isOnline(recipientId)) {
            System.out.println("Friend request update for offline user " + recipientId + ": " + response.getStatus());
        }
    }

    private void validateDifferentUsers(UUID userA, UUID userB) {
        if (userA.equals(userB)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot send a friend request to yourself");
        }
    }

    private UUID canonicalLow(UUID a, UUID b) {
        return a.compareTo(b) <= 0 ? a : b;
    }

    private UUID canonicalHigh(UUID a, UUID b) {
        return a.compareTo(b) <= 0 ? b : a;
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}

