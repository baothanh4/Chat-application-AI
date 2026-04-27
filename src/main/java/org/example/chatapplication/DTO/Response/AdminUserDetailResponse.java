package org.example.chatapplication.DTO.Response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.chatapplication.Model.Enum.UserRole;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AdminUserDetailResponse {
    private UUID userId;
    private String username;
    private String displayName;
    private String fullName;
    private String phoneNumber;
    private String address;
    private String bio;
    private LocalDate dateOfBirth;
    private String gender;
    private String email;
    private String avatarPath;
    private UserRole role;
    private boolean accountLocked;
    private Instant lastSeenAt;
    private Instant createdAt;
    private Instant updatedAt;
}

