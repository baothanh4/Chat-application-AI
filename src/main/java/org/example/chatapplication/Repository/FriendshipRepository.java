package org.example.chatapplication.Repository;

import org.example.chatapplication.Model.Entity.Friendship;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface FriendshipRepository extends JpaRepository<Friendship, UUID> {
    boolean existsByUserLowIdAndUserHighId(UUID userLowId, UUID userHighId);

    List<Friendship> findByUserLowIdOrUserHighIdOrderByCreatedAtDesc(UUID userLowId, UUID userHighId);
}

