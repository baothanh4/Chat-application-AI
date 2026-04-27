package org.example.chatapplication.Repository;

import org.example.chatapplication.Model.Entity.MessageReaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MessageReactionRepository extends JpaRepository<MessageReaction, UUID> {
    List<MessageReaction> findByMessageIdOrderByCreatedAtAsc(UUID messageId);

    Optional<MessageReaction> findByMessageIdAndUserId(UUID messageId, UUID userId);

    void deleteByMessageIdAndUserId(UUID messageId, UUID userId);
}

