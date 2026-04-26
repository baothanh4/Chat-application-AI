package org.example.chatapplication.Service;

import lombok.RequiredArgsConstructor;
import org.example.chatapplication.DTO.Request.CreateModerationReportRequest;
import org.example.chatapplication.DTO.Request.UpdateModerationReportStatusRequest;
import org.example.chatapplication.DTO.Response.ModerationReportResponse;
import org.example.chatapplication.Model.Entity.ModerationReport;
import org.example.chatapplication.Model.Entity.UserAccount;
import org.example.chatapplication.Model.Enum.ModerationReportStatus;
import org.example.chatapplication.Repository.ModerationReportRepository;
import org.example.chatapplication.Repository.UserAccountRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ModerationReportService {

    private final ModerationReportRepository moderationReportRepository;
    private final UserAccountRepository userAccountRepository;

    @Transactional
    public ModerationReportResponse createReport(String reporterUsername, CreateModerationReportRequest request) {
        UserAccount reporter = requireUserByUsername(reporterUsername);
        ModerationReport report = new ModerationReport();
        report.setReporter(reporter);
        report.setTargetUser(resolveOptionalUser(request.getTargetUserId()));
        report.setConversationId(request.getConversationId());
        report.setMessageId(request.getMessageId());
        report.setReason(request.getReason().trim());
        report.setDetails(trimToNull(request.getDetails()));
        report.setStatus(ModerationReportStatus.OPEN);

        return toResponse(moderationReportRepository.save(report));
    }

    @Transactional(readOnly = true)
    public List<ModerationReportResponse> listReports(ModerationReportStatus status) {
        List<ModerationReport> reports = status == null
                ? moderationReportRepository.findAllByOrderByCreatedAtDesc()
                : moderationReportRepository.findByStatusOrderByCreatedAtDesc(status);
        return reports.stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<ModerationReportResponse> listMyReports(String username) {
        UserAccount user = requireUserByUsername(username);
        return moderationReportRepository.findByReporterIdOrderByCreatedAtDesc(user.getId())
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public ModerationReportResponse updateReportStatus(UUID reportId,
                                                       String reviewerUsername,
                                                       UpdateModerationReportStatusRequest request) {
        ModerationReport report = moderationReportRepository.findById(reportId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Moderation report not found: " + reportId));

        UserAccount reviewer = requireUserByUsername(reviewerUsername);
        validateStatusTransition(report.getStatus(), request.getStatus());
        report.setStatus(request.getStatus());
        report.setReviewedBy(reviewer);
        report.setReviewedAt(Instant.now());
        report.setModeratorNote(trimToNull(request.getModeratorNote()));

        return toResponse(moderationReportRepository.save(report));
    }

    private UserAccount requireUserByUsername(String username) {
        return userAccountRepository.findByUsernameIgnoreCase(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found: " + username));
    }

    private UserAccount resolveOptionalUser(UUID userId) {
        if (userId == null) {
            return null;
        }
        return userAccountRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found: " + userId));
    }

    private ModerationReportResponse toResponse(ModerationReport report) {
        return new ModerationReportResponse(
                report.getId(),
                report.getReporter().getId(),
                report.getReporter().getUsername(),
                report.getTargetUser() == null ? null : report.getTargetUser().getId(),
                report.getConversationId(),
                report.getMessageId(),
                report.getReason(),
                report.getDetails(),
                report.getStatus(),
                report.getReviewedBy() == null ? null : report.getReviewedBy().getId(),
                report.getReviewedBy() == null ? null : report.getReviewedBy().getUsername(),
                report.getReviewedAt(),
                report.getModeratorNote(),
                report.getCreatedAt()
        );
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private void validateStatusTransition(ModerationReportStatus currentStatus, ModerationReportStatus nextStatus) {
        if (currentStatus == null || nextStatus == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Report status is required");
        }

        if (currentStatus == nextStatus) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Report is already in status " + currentStatus.name());
        }

        if (currentStatus == ModerationReportStatus.OPEN &&
                (nextStatus == ModerationReportStatus.IN_REVIEW
                        || nextStatus == ModerationReportStatus.RESOLVED
                        || nextStatus == ModerationReportStatus.REJECTED)) {
            return;
        }

        if (currentStatus == ModerationReportStatus.IN_REVIEW &&
                (nextStatus == ModerationReportStatus.RESOLVED || nextStatus == ModerationReportStatus.REJECTED)) {
            return;
        }

        throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "Invalid moderation status transition from " + currentStatus.name() + " to " + nextStatus.name());
    }
}

