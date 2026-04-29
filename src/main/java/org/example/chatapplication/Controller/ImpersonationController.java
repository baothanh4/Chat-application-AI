package org.example.chatapplication.Controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.chatapplication.DTO.Request.ImpersonateUserRequest;
import org.example.chatapplication.DTO.Response.ImpersonationTokenResponse;
import org.example.chatapplication.Service.ImpersonationService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/impersonate")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class ImpersonationController {

    private final ImpersonationService impersonationService;

    @PostMapping("/start")
    ResponseEntity<ImpersonationTokenResponse> startImpersonation(
            @RequestBody @Valid ImpersonateUserRequest request,
            Authentication authentication) {
        ImpersonationTokenResponse response = impersonationService.createImpersonationToken(
                request.getTargetUserId(),
                authentication.getName(),
                request.getReason()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/stop")
    ResponseEntity<Void> stopImpersonation(@RequestParam String token, Authentication authentication) {
        impersonationService.revokeImpersonationToken(token, authentication.getName());
        return ResponseEntity.noContent().build();
    }
}

