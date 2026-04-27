package org.example.chatapplication.Repository;

import org.example.chatapplication.Model.Entity.ChatMessage;
import org.example.chatapplication.Model.Enum.MessageType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, UUID> {
    Page<ChatMessage> findByConversationIdOrderByCreatedAtDesc(UUID conversationId, Pageable pageable);

    ChatMessage findTopByConversationIdOrderByCreatedAtDesc(UUID conversationId);

    List<ChatMessage> findByConversationIdAndPinnedTrueOrderByPinnedAtDescCreatedAtDesc(UUID conversationId);

    Page<ChatMessage> findByConversationIdAndMessageTypeAndUnsentFalseAndDeletedForEveryoneFalseOrderByCreatedAtDesc(
            UUID conversationId,
            MessageType messageType,
            Pageable pageable
    );

    long countByConversationIdAndSender_IdNot(UUID conversationId, UUID senderId);

    long countByConversationIdAndSender_IdNotAndCreatedAtAfter(UUID conversationId, UUID senderId, Instant createdAt);
}
