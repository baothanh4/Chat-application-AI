package org.example.chatapplication.Model.Entity;

import jakarta.persistence.*;
import lombok.*;

import org.example.chatapplication.Model.Enum.MessageStatus;
import org.example.chatapplication.Model.Enum.MessageType;
import org.example.chatapplication.Model.Entity.UserAccount;

import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "chat_messages")
public class ChatMessage extends BaseEntity{
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "conversation_id", nullable = false)
    @ToString.Exclude
    private Conversation conversation;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sender_id", nullable = false)
    @ToString.Exclude
    private UserAccount sender;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MessageType messageType = MessageType.TEXT;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MessageStatus status = MessageStatus.QUEUED;

    @Column(nullable = false, length = 4000)
    private String content;

    @Column(length = 100)
    private String clientMessageId;

    @Column(name = "reply_to_message_id")
    private UUID replyToMessageId;

    private boolean edited = false;

    private Instant editedAt;

    private boolean unsent = false;

    private Instant unsentAt;

    private boolean deletedForEveryone = false;

    private Instant deletedAt;

    @Column(name = "deleted_by_user_id")
    private UUID deletedByUserId;

    private boolean pinned = false;

    private Instant pinnedAt;

    @Column(name = "pinned_by_user_id")
    private UUID pinnedByUserId;

    private Instant deliveredAt;

    private Instant readAt;

    @OneToMany(mappedBy = "message", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @ToString.Exclude
    private Set<MessageReaction> reactions = new LinkedHashSet<>();
}
