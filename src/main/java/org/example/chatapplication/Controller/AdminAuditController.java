package org.example.chatapplication.Controller;

import lombok.RequiredArgsConstructor;
import org.example.chatapplication.DTO.Response.AdminAuditLogResponse;
import org.example.chatapplication.Service.AdminAuditService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/audits")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminAuditController {

    private final AdminAuditService adminAuditService;

    @GetMapping
    ResponseEntity<List<AdminAuditLogResponse>> getLatestAudits(@RequestParam(defaultValue = "100") int limit) {
        return ResponseEntity.ok(adminAuditService.getLatest(limit));
    }
}

