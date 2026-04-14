package org.example.chatapplication.Repository;

import org.example.chatapplication.Model.Entity.FriendRequest;
import org.example.chatapplication.Model.Enum.FriendRequestStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface FriendRequestRepository extends JpaRepository<FriendRequest, UUID> {
    List<FriendRequest> findByRecipientIdAndStatusOrderByCreatedAtDesc(UUID recipientId, FriendRequestStatus status);

    List<FriendRequest> findByRequesterIdAndStatusOrderByCreatedAtDesc(UUID requesterId, FriendRequestStatus status);

    Optional<FriendRequest> findByIdAndRecipientId(UUID id, UUID recipientId);

    Optional<FriendRequest> findByIdAndRequesterId(UUID id, UUID requesterId);

    @Query("""
            select fr from FriendRequest fr
            where (fr.requester.id = :userA and fr.recipient.id = :userB)
               or (fr.requester.id = :userB and fr.recipient.id = :userA)
            order by fr.createdAt desc
            """)
    List<FriendRequest> findBetweenUsers(@Param("userA") UUID userA, @Param("userB") UUID userB);
}

