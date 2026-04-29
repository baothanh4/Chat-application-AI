package org.example.chatapplication.DTO.Response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ImpersonationTokenResponse {
    private String token;
    private Instant expiresAt;
    private String targetUsername;
    private String message;
}

