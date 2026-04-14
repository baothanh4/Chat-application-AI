package org.example.chatapplication.Model.Entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "conversation_ai_insights")
public class ConversationAiInsight extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "conversation_id", nullable = false, unique = true)
    @ToString.Exclude
    private Conversation conversation;

    @Column(nullable = false, length = 4000)
    private String summary;

    @Column(length = 4000)
    private String summaryBullets;

    @Column(length = 255)
    private String focusTopic;

    @Column(length = 120)
    private String modelName;

    @Column(name = "generated_at", nullable = false)
    private Instant generatedAt;

    @Column(name = "source_message_count", nullable = false)
    private long sourceMessageCount;

    @Column(name = "source_latest_message_at")
    private Instant sourceLatestMessageAt;

    @OneToMany(mappedBy = "insight", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @ToString.Exclude
    private List<ConversationTaskInsight> tasks = new ArrayList<>();
}

