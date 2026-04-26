package org.example.chatapplication.Service;

import org.example.chatapplication.DTO.Response.AdminOverviewResponse;
import org.example.chatapplication.Model.Enum.ModerationReportStatus;
import org.example.chatapplication.Repository.ChatMessageRepository;
import org.example.chatapplication.Repository.ConversationRepository;
import org.example.chatapplication.Repository.ModerationReportRepository;
import org.example.chatapplication.Repository.UserAccountRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminOverviewServiceTest {

    @Mock
    private UserAccountRepository userAccountRepository;

    @Mock
    private ConversationRepository conversationRepository;

    @Mock
    private ChatMessageRepository chatMessageRepository;

    @Mock
    private ModerationReportRepository moderationReportRepository;

    @InjectMocks
    private AdminOverviewService adminOverviewService;

    @Test
    void getOverviewShouldReturnAggregatedMetrics() {
        when(userAccountRepository.count()).thenReturn(120L);
        when(userAccountRepository.countByLastSeenAtAfter(any())).thenReturn(37L);
        when(userAccountRepository.countByAccountLockedTrue()).thenReturn(4L);
        when(conversationRepository.count()).thenReturn(52L);
        when(chatMessageRepository.count()).thenReturn(1450L);
        when(moderationReportRepository.countByStatus(ModerationReportStatus.OPEN)).thenReturn(5L);
        when(moderationReportRepository.countByStatus(ModerationReportStatus.IN_REVIEW)).thenReturn(3L);
        when(moderationReportRepository.countByStatus(ModerationReportStatus.RESOLVED)).thenReturn(20L);
        when(moderationReportRepository.countByStatus(ModerationReportStatus.REJECTED)).thenReturn(2L);

        AdminOverviewResponse response = adminOverviewService.getOverview();

        assertThat(response.getTotalUsers()).isEqualTo(120L);
        assertThat(response.getActiveUsersLast15Minutes()).isEqualTo(37L);
        assertThat(response.getLockedUsers()).isEqualTo(4L);
        assertThat(response.getTotalConversations()).isEqualTo(52L);
        assertThat(response.getTotalMessages()).isEqualTo(1450L);
        assertThat(response.getOpenReports()).isEqualTo(5L);
        assertThat(response.getInReviewReports()).isEqualTo(3L);
        assertThat(response.getResolvedReports()).isEqualTo(20L);
        assertThat(response.getRejectedReports()).isEqualTo(2L);
    }
}

