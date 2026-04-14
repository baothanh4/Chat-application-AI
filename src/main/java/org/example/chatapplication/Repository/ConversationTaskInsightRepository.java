package org.example.chatapplication.Repository;

import org.example.chatapplication.Model.Entity.ConversationTaskInsight;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ConversationTaskInsightRepository extends JpaRepository<ConversationTaskInsight, UUID> {
    @Query("""
            select t
            from ConversationTaskInsight t
            join t.insight i
            join i.conversation c
            join c.members m
            where m.user.id = :userId
            """)
    List<ConversationTaskInsight> findAllForUser(@Param("userId") UUID userId);
}

