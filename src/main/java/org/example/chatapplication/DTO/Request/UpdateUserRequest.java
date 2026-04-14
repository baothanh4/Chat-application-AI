package org.example.chatapplication.DTO.Request;

import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class UpdateUserRequest {
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
}
