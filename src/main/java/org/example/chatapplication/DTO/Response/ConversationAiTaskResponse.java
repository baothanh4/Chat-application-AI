package org.example.chatapplication.DTO.Response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ConversationAiTaskResponse {
    private UUID taskId;
    private UUID conversationId;
    private String conversationName;
    private String title;
    private String description;
    private String sourceMessageSnippet;
    private Instant dueAt;
    private String priority;
    private String status;
    private double confidenceScore;
    private long daysRemaining;
    private boolean overdue;
}

