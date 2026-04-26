package org.example.chatapplication.Model.Entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.example.chatapplication.Model.Enum.ModerationReportStatus;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "moderation_reports")
public class ModerationReport extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "reporter_id", nullable = false)
    private UserAccount reporter;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "target_user_id")
    private UserAccount targetUser;

    @Column(name = "conversation_id")
    private UUID conversationId;

    @Column(name = "message_id")
    private UUID messageId;

    @Column(nullable = false, length = 200)
    private String reason;

    @Column(length = 2000)
    private String details;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ModerationReportStatus status = ModerationReportStatus.OPEN;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewed_by")
    private UserAccount reviewedBy;

    @Column(name = "reviewed_at")
    private Instant reviewedAt;

    @Column(name = "moderator_note", length = 2000)
    private String moderatorNote;
}

