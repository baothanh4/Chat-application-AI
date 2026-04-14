package org.example.chatapplication.DTO.Response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class FaceLoginCandidateResponse {
    private UUID id;
    private String username;
    private String displayName;
    private String avatarPath;
    private int distance;
}

