package org.example.chatapplication.Service;

import lombok.RequiredArgsConstructor;
import org.example.chatapplication.DTO.Response.UserSearchResponse;
import org.example.chatapplication.Model.Entity.FriendRequest;
import org.example.chatapplication.Model.Entity.UserAccount;
import org.example.chatapplication.Model.Enum.FriendRequestStatus;
import org.example.chatapplication.Repository.FriendRequestRepository;
import org.example.chatapplication.Repository.FriendshipRepository;
import org.example.chatapplication.Repository.UserAccountRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserDirectoryService {
    private final UserAccountRepository userAccountRepository;
    private final FriendshipRepository friendshipRepository;
    private final FriendRequestRepository friendRequestRepository;
    private final PresenceService presenceService;

    @Transactional(readOnly = true)
    public List<UserSearchResponse> searchSuggestions(String query, int limit, UUID viewerId) {
        if (!StringUtils.hasText(query)) {
            return List.of();
        }

        int safeLimit = Math.max(1, Math.min(limit, 20));
        Pageable pageable = PageRequest.of(0, safeLimit);
        String normalizedQuery = query.trim();

        return userAccountRepository.searchByDisplayNameOrUsername(normalizedQuery, pageable).stream()
                .filter(user -> viewerId == null || !viewerId.equals(user.getId()))
                .sorted(Comparator.comparing(UserAccount::getDisplayName, String.CASE_INSENSITIVE_ORDER)
                        .thenComparing(UserAccount::getUsername, String.CASE_INSENSITIVE_ORDER))
                .map(user -> toSearchResponse(user, viewerId))
                .toList();
    }

    @Transactional(readOnly = true)
    public UserSearchResponse toSearchResponse(UserAccount user, UUID viewerId) {
        boolean alreadyFriend = viewerId != null && isFriend(viewerId, user.getId());
        FriendRelationshipState relationshipState = resolveRelationshipState(viewerId, user.getId(), alreadyFriend);
        UUID pendingRequestId = resolvePendingRequestId(viewerId, user.getId(), relationshipState);

        return new UserSearchResponse(
                user.getId(),
                user.getUsername(),
                user.getDisplayName(),
                user.getAvatarPath(),
                presenceService.getPresence(user.getId()),
                alreadyFriend,
                relationshipState.name(),
                pendingRequestId
        );
    }

    @Transactional(readOnly = true)
    public UserAccount requireUser(UUID userId) {
        return userAccountRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found: " + userId));
    }

    private boolean isFriend(UUID userA, UUID userB) {
        UUID low = canonicalLow(userA, userB);
        UUID high = canonicalHigh(userA, userB);
        return friendshipRepository.existsByUserLowIdAndUserHighId(low, high);
    }

    private FriendRelationshipState resolveRelationshipState(UUID viewerId, UUID otherUserId, boolean alreadyFriend) {
        if (viewerId == null) {
            return FriendRelationshipState.NONE;
        }
        if (alreadyFriend) {
            return FriendRelationshipState.FRIEND;
        }

        List<FriendRequest> requests = friendRequestRepository.findBetweenUsers(viewerId, otherUserId);
        if (requests.isEmpty()) {
            return FriendRelationshipState.NONE;
        }

        FriendRequest latest = requests.getFirst();
        if (latest.getStatus() != FriendRequestStatus.PENDING) {
            return FriendRelationshipState.NONE;
        }

        if (latest.getRequester() != null && viewerId.equals(latest.getRequester().getId())) {
            return FriendRelationshipState.PENDING_OUT;
        }
        if (latest.getRecipient() != null && viewerId.equals(latest.getRecipient().getId())) {
            return FriendRelationshipState.PENDING_IN;
        }
        return FriendRelationshipState.NONE;
    }

    private UUID resolvePendingRequestId(UUID viewerId, UUID otherUserId, FriendRelationshipState relationshipState) {
        if (viewerId == null || relationshipState == FriendRelationshipState.NONE || relationshipState == FriendRelationshipState.FRIEND) {
            return null;
        }

        return friendRequestRepository.findBetweenUsers(viewerId, otherUserId).stream()
                .filter(request -> request.getStatus() == FriendRequestStatus.PENDING)
                .filter(request -> (relationshipState == FriendRelationshipState.PENDING_OUT && request.getRequester() != null && viewerId.equals(request.getRequester().getId()))
                        || (relationshipState == FriendRelationshipState.PENDING_IN && request.getRecipient() != null && viewerId.equals(request.getRecipient().getId())))
                .map(FriendRequest::getId)
                .findFirst()
                .orElse(null);
    }

    private UUID canonicalLow(UUID a, UUID b) {
        return a.compareTo(b) <= 0 ? a : b;
    }

    private UUID canonicalHigh(UUID a, UUID b) {
        return a.compareTo(b) <= 0 ? b : a;
    }

    private enum FriendRelationshipState {
        NONE,
        PENDING_OUT,
        PENDING_IN,
        FRIEND
    }
}

