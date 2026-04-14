package org.example.chatapplication.DTO.Response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.Instant;
import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserResponse {
    UUID id;
    String username;
    String displayName;
    String fullName;
    String phoneNumber;
    String address;
    String bio;
    LocalDate dateOfBirth;
    String gender;
    String email;
    String avatarPath;
    String faceTemplatePath;
    boolean faceLoginEnabled;
    Instant faceEnrolledAt;
    PresenceResponse presence;
}
