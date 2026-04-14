package org.example.chatapplication.Repository;

import org.example.chatapplication.Model.Entity.ConversationAiInsight;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ConversationAiInsightRepository extends JpaRepository<ConversationAiInsight, UUID> {
    Optional<ConversationAiInsight> findByConversationId(UUID conversationId);
}

