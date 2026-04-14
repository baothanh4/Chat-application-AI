package org.example.chatapplication.DTO.Response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ConversationAiInsightResponse {
    private UUID conversationId;
    private String conversationName;
    private String summary;
    private List<String> summaryBullets;
    private String focusTopic;
    private Instant generatedAt;
    private Instant sourceLatestMessageAt;
    private long sourceMessageCount;
    private Instant nextDeadlineAt;
    private long openTaskCount;
    private long overdueTaskCount;
    private List<ConversationAiTaskResponse> tasks;
}

