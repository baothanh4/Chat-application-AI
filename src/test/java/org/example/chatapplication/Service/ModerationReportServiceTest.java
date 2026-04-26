package org.example.chatapplication.Service;

import org.example.chatapplication.DTO.Request.UpdateModerationReportStatusRequest;
import org.example.chatapplication.DTO.Response.ModerationReportResponse;
import org.example.chatapplication.Model.Entity.ModerationReport;
import org.example.chatapplication.Model.Entity.UserAccount;
import org.example.chatapplication.Model.Enum.ModerationReportStatus;
import org.example.chatapplication.Repository.ModerationReportRepository;
import org.example.chatapplication.Repository.UserAccountRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ModerationReportServiceTest {

    @Mock
    private ModerationReportRepository moderationReportRepository;

    @Mock
    private UserAccountRepository userAccountRepository;

    @InjectMocks
    private ModerationReportService moderationReportService;

    @Test
    void updateReportStatusShouldAllowOpenToInReview() {
        UUID reportId = UUID.randomUUID();
        UserAccount reporter = user(UUID.randomUUID(), "reporter");
        UserAccount moderator = user(UUID.randomUUID(), "moderator");
        ModerationReport report = report(reportId, reporter, ModerationReportStatus.OPEN);

        when(moderationReportRepository.findById(reportId)).thenReturn(Optional.of(report));
        when(userAccountRepository.findByUsernameIgnoreCase("moderator")).thenReturn(Optional.of(moderator));
        when(moderationReportRepository.save(any(ModerationReport.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UpdateModerationReportStatusRequest request = new UpdateModerationReportStatusRequest();
        request.setStatus(ModerationReportStatus.IN_REVIEW);
        request.setModeratorNote("Checking report");

        ModerationReportResponse response = moderationReportService.updateReportStatus(reportId, "moderator", request);

        assertThat(response.getStatus()).isEqualTo(ModerationReportStatus.IN_REVIEW);
        assertThat(response.getReviewedByUsername()).isEqualTo("moderator");
        assertThat(response.getModeratorNote()).isEqualTo("Checking report");
    }

    @Test
    void updateReportStatusShouldRejectSameStatus() {
        UUID reportId = UUID.randomUUID();
        UserAccount reporter = user(UUID.randomUUID(), "reporter");
        UserAccount moderator = user(UUID.randomUUID(), "moderator");
        ModerationReport report = report(reportId, reporter, ModerationReportStatus.IN_REVIEW);

        when(moderationReportRepository.findById(reportId)).thenReturn(Optional.of(report));
        when(userAccountRepository.findByUsernameIgnoreCase("moderator")).thenReturn(Optional.of(moderator));

        UpdateModerationReportStatusRequest request = new UpdateModerationReportStatusRequest();
        request.setStatus(ModerationReportStatus.IN_REVIEW);

        assertThatThrownBy(() -> moderationReportService.updateReportStatus(reportId, "moderator", request))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> {
                    ResponseStatusException responseException = (ResponseStatusException) ex;
                    assertThat(responseException.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
                });
    }

    @Test
    void updateReportStatusShouldRejectTransitionFromResolvedBackToInReview() {
        UUID reportId = UUID.randomUUID();
        UserAccount reporter = user(UUID.randomUUID(), "reporter");
        UserAccount moderator = user(UUID.randomUUID(), "moderator");
        ModerationReport report = report(reportId, reporter, ModerationReportStatus.RESOLVED);

        when(moderationReportRepository.findById(reportId)).thenReturn(Optional.of(report));
        when(userAccountRepository.findByUsernameIgnoreCase("moderator")).thenReturn(Optional.of(moderator));

        UpdateModerationReportStatusRequest request = new UpdateModerationReportStatusRequest();
        request.setStatus(ModerationReportStatus.IN_REVIEW);

        assertThatThrownBy(() -> moderationReportService.updateReportStatus(reportId, "moderator", request))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> {
                    ResponseStatusException responseException = (ResponseStatusException) ex;
                    assertThat(responseException.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
                });
    }

    private UserAccount user(UUID id, String username) {
        UserAccount user = new UserAccount();
        user.setId(id);
        user.setUsername(username);
        return user;
    }

    private ModerationReport report(UUID id, UserAccount reporter, ModerationReportStatus status) {
        ModerationReport report = new ModerationReport();
        report.setId(id);
        report.setReporter(reporter);
        report.setStatus(status);
        report.setReason("Spam");
        return report;
    }
}

