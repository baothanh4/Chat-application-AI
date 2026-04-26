package org.example.chatapplication.Service;

import lombok.RequiredArgsConstructor;
import org.example.chatapplication.DTO.Response.AdminOverviewResponse;
import org.example.chatapplication.Model.Enum.ModerationReportStatus;
import org.example.chatapplication.Repository.ChatMessageRepository;
import org.example.chatapplication.Repository.ConversationRepository;
import org.example.chatapplication.Repository.ModerationReportRepository;
import org.example.chatapplication.Repository.UserAccountRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Service
@RequiredArgsConstructor
public class AdminOverviewService {

    private final UserAccountRepository userAccountRepository;
    private final ConversationRepository conversationRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final ModerationReportRepository moderationReportRepository;

    @Transactional(readOnly = true)
    public AdminOverviewResponse getOverview() {
        Instant activeThreshold = Instant.now().minus(15, ChronoUnit.MINUTES);

        return new AdminOverviewResponse(
                userAccountRepository.count(),
                userAccountRepository.countByLastSeenAtAfter(activeThreshold),
                userAccountRepository.countByAccountLockedTrue(),
                conversationRepository.count(),
                chatMessageRepository.count(),
                moderationReportRepository.countByStatus(ModerationReportStatus.OPEN),
                moderationReportRepository.countByStatus(ModerationReportStatus.IN_REVIEW),
                moderationReportRepository.countByStatus(ModerationReportStatus.RESOLVED),
                moderationReportRepository.countByStatus(ModerationReportStatus.REJECTED)
        );
    }
}

