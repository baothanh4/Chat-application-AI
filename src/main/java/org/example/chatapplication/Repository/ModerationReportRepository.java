package org.example.chatapplication.Repository;

import org.example.chatapplication.Model.Entity.ModerationReport;
import org.example.chatapplication.Model.Enum.ModerationReportStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ModerationReportRepository extends JpaRepository<ModerationReport, UUID> {
    List<ModerationReport> findByReporterIdOrderByCreatedAtDesc(UUID reporterId);

    List<ModerationReport> findByStatusOrderByCreatedAtDesc(ModerationReportStatus status);

    long countByStatus(ModerationReportStatus status);

    List<ModerationReport> findAllByOrderByCreatedAtDesc();
}

