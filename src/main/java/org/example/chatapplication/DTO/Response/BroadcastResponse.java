package org.example.chatapplication.DTO.Response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class BroadcastResponse {
    private UUID id;
    private String title;
    private String message;
    private String linkUrl;
    private UUID createdByUserId;
    private String createdByUsername;
    private Instant createdAt;
}

