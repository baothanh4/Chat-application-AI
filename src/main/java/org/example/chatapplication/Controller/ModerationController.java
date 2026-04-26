package org.example.chatapplication.Controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.chatapplication.DTO.Request.CreateModerationReportRequest;
import org.example.chatapplication.DTO.Request.UpdateModerationReportStatusRequest;
import org.example.chatapplication.DTO.Response.ModerationReportResponse;
import org.example.chatapplication.Model.Enum.ModerationReportStatus;
import org.example.chatapplication.Service.ModerationReportService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/moderation")
@RequiredArgsConstructor
public class ModerationController {

    private final ModerationReportService moderationReportService;

    @PostMapping("/reports")
    @PreAuthorize("isAuthenticated()")
    ResponseEntity<ModerationReportResponse> createReport(@RequestBody @Valid CreateModerationReportRequest request,
                                                          Authentication authentication) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(moderationReportService.createReport(authentication.getName(), request));
    }

    @GetMapping("/reports/mine")
    @PreAuthorize("isAuthenticated()")
    ResponseEntity<List<ModerationReportResponse>> listMyReports(Authentication authentication) {
        return ResponseEntity.ok(moderationReportService.listMyReports(authentication.getName()));
    }

    @GetMapping("/reports")
    @PreAuthorize("hasAnyRole('MODERATOR','ADMIN')")
    ResponseEntity<List<ModerationReportResponse>> listReports(@RequestParam(required = false) ModerationReportStatus status) {
        return ResponseEntity.ok(moderationReportService.listReports(status));
    }

    @PatchMapping("/reports/{reportId}")
    @PreAuthorize("hasAnyRole('MODERATOR','ADMIN')")
    ResponseEntity<ModerationReportResponse> updateReportStatus(@PathVariable UUID reportId,
                                                                @RequestBody @Valid UpdateModerationReportStatusRequest request,
                                                                Authentication authentication) {
        return ResponseEntity.ok(moderationReportService.updateReportStatus(reportId, authentication.getName(), request));
    }
}

