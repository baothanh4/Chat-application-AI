package org.example.chatapplication.Repository;

import org.example.chatapplication.Model.Entity.CallHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface CallHistoryRepository extends JpaRepository<CallHistory, UUID> {
    Page<CallHistory> findByConversationIdOrderByStartedAtDesc(UUID conversationId, Pageable pageable);

    @Query("select c from CallHistory c where c.caller.id = :userId or c.callee.id = :userId order by c.startedAt desc")
    Page<CallHistory> findByUserId(UUID userId, Pageable pageable);
}

