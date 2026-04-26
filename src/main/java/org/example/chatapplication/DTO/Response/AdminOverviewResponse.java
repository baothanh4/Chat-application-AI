package org.example.chatapplication.DTO.Response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AdminOverviewResponse {
    private long totalUsers;
    private long activeUsersLast15Minutes;
    private long lockedUsers;
    private long totalConversations;
    private long totalMessages;
    private long openReports;
    private long inReviewReports;
    private long resolvedReports;
    private long rejectedReports;
}

