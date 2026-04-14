package org.example.chatapplication.Model.Entity;

import jakarta.persistence.*;
import lombok.*;
import org.example.chatapplication.Model.Enum.ConversationRole;



import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(
        name = "conversation_members",
        uniqueConstraints = @UniqueConstraint(name = "uk_conversation_member", columnNames = {"conversation_id", "user_id"})
)
public class ConversationMember extends BaseEntity{
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "conversation_id", nullable = false)
    @ToString.Exclude
    private Conversation conversation;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    @ToString.Exclude
    private UserAccount user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ConversationRole role = ConversationRole.MEMBER;

    @Column
    private Instant lastReadAt;

    @Column(nullable = false)
    private boolean muted = false;
}
