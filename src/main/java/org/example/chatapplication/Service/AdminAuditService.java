package org.example.chatapplication.Service;

import lombok.RequiredArgsConstructor;
import org.example.chatapplication.DTO.Response.AdminAuditLogResponse;
import org.example.chatapplication.Model.Entity.AdminAuditLog;
import org.example.chatapplication.Model.Enum.AdminAuditAction;
import org.example.chatapplication.Repository.AdminAuditLogRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AdminAuditService {

    private final AdminAuditLogRepository adminAuditLogRepository;

    @Transactional
    public void log(AdminAuditAction action, String actorUsername, UUID targetUserId, String details) {
        AdminAuditLog auditLog = new AdminAuditLog();
        auditLog.setAction(action);
        auditLog.setActorUsername(actorUsername == null ? "unknown" : actorUsername.trim());
        auditLog.setTargetUserId(targetUserId);
        auditLog.setDetails(trimToNull(details));
        adminAuditLogRepository.save(auditLog);
    }

    @Transactional(readOnly = true)
    public List<AdminAuditLogResponse> getLatest(int limit) {
        int normalizedLimit = Math.max(1, Math.min(limit, 200));
        return adminAuditLogRepository.findAllByOrderByCreatedAtDesc(PageRequest.of(0, normalizedLimit))
                .stream()
                .map(this::toResponse)
                .toList();
    }

    private AdminAuditLogResponse toResponse(AdminAuditLog auditLog) {
        return new AdminAuditLogResponse(
                auditLog.getId(),
                auditLog.getAction(),
                auditLog.getActorUsername(),
                auditLog.getTargetUserId(),
                auditLog.getDetails(),
                auditLog.getCreatedAt()
        );
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}

