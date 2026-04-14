package org.example.chatapplication.Repository;

import org.example.chatapplication.Model.Entity.Conversation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface ConversationRepository extends JpaRepository<Conversation, UUID> {
    @Query("select distinct c from Conversation c join c.members m where m.user.id = :userId order by c.updatedAt desc")
    Page<Conversation> findConversationsByUserId(@Param("userId") UUID userId, Pageable pageable);

    @Query("""
            select distinct c
            from Conversation c
            join c.members m1
            join c.members m2
            where c.type = org.example.chatapplication.Model.Enum.ConversationType.PRIVATE
              and ((m1.user.id = :userId1 and m2.user.id = :userId2)
                or (m1.user.id = :userId2 and m2.user.id = :userId1))
            """)
    java.util.List<Conversation> findPrivateConversationBetweenUsers(@Param("userId1") UUID userId1,
                                                                     @Param("userId2") UUID userId2);
}
