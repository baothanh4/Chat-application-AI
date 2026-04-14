package org.example.chatapplication.Model.Entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.example.chatapplication.Model.Enum.FriendRequestStatus;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(
        name = "friend_requests",
        indexes = {
                @Index(name = "idx_friend_requests_requester_status", columnList = "requester_id,status"),
                @Index(name = "idx_friend_requests_recipient_status", columnList = "recipient_id,status")
        }
)
public class FriendRequest extends BaseEntity {
    @ManyToOne(optional = false)
    @JoinColumn(name = "requester_id", nullable = false, updatable = false)
    private UserAccount requester;

    @ManyToOne(optional = false)
    @JoinColumn(name = "recipient_id", nullable = false, updatable = false)
    private UserAccount recipient;

    @Column(length = 500)
    private String message;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private FriendRequestStatus status = FriendRequestStatus.PENDING;

    @Column(name = "responded_at")
    private Instant respondedAt;

    @ManyToOne
    @JoinColumn(name = "responded_by_id")
    private UserAccount respondedBy;

    @Column(name = "private_conversation_id")
    private UUID privateConversationId;
}

