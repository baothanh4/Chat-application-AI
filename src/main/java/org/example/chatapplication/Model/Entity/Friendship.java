package org.example.chatapplication.Model.Entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(
        name = "friendships",
        indexes = {
                @Index(name = "idx_friendships_user_low", columnList = "user_low_id"),
                @Index(name = "idx_friendships_user_high", columnList = "user_high_id")
        }
)
public class Friendship extends BaseEntity {
    @ManyToOne(optional = false)
    @JoinColumn(name = "user_low_id", nullable = false, updatable = false)
    private UserAccount userLow;

    @ManyToOne(optional = false)
    @JoinColumn(name = "user_high_id", nullable = false, updatable = false)
    private UserAccount userHigh;

    @Column(name = "friend_request_id")
    private UUID friendRequestId;

    @Column(name = "private_conversation_id")
    private UUID privateConversationId;
}

