package org.example.chatapplication.Controller;

import lombok.RequiredArgsConstructor;
import org.example.chatapplication.DTO.Response.AdminOverviewResponse;
import org.example.chatapplication.Service.AdminOverviewService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/overview")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminOverviewController {

    private final AdminOverviewService adminOverviewService;

    @GetMapping
    ResponseEntity<AdminOverviewResponse> getOverview() {
        return ResponseEntity.ok(adminOverviewService.getOverview());
    }
}

