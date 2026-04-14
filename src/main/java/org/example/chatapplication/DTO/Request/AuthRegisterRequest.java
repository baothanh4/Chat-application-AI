package org.example.chatapplication.DTO.Request;

import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AuthRegisterRequest {
    @NotBlank
    @Size(max = 80)
    private String username;

    @NotBlank
    @Size(max = 120)
    private String displayName;

    @Size(max = 120)
    private String fullName;

    @Size(max = 20)
    private String phoneNumber;

    @Size(max = 255)
    private String address;

    @Size(max = 500)
    private String bio;

    @Past
    private LocalDate dateOfBirth;

    @Size(max = 20)
    private String gender;

    @Email
    @Size(max = 160)
    private String email;

    @Size(max = 255)
    private String avatarPath;

    @NotBlank
    @Size(min = 6, max = 72)
    private String password;
}

