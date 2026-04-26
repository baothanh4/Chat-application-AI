package org.example.chatapplication.Controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.chatapplication.DTO.Request.UpdateGlobalAiPolicyRequest;
import org.example.chatapplication.DTO.Response.GlobalAiPolicyResponse;
import org.example.chatapplication.Model.Enum.AdminAuditAction;
import org.example.chatapplication.Service.AdminAuditService;
import org.example.chatapplication.Service.GlobalAiPolicyService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ai-policy")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class GlobalAiPolicyController {

    private final GlobalAiPolicyService globalAiPolicyService;
    private final AdminAuditService adminAuditService;

    @GetMapping
    ResponseEntity<GlobalAiPolicyResponse> getPolicy() {
        return ResponseEntity.ok(globalAiPolicyService.getCurrentPolicyResponse());
    }

    @PutMapping
    ResponseEntity<GlobalAiPolicyResponse> updatePolicy(@RequestBody @Valid UpdateGlobalAiPolicyRequest request,
                                                        Authentication authentication) {
        GlobalAiPolicyResponse response = globalAiPolicyService.updatePolicy(request);
        adminAuditService.log(
                AdminAuditAction.AI_POLICY_UPDATED,
                authentication == null ? "unknown" : authentication.getName(),
                null,
                "Updated global AI policy"
        );
        return ResponseEntity.ok(response);
    }
}

