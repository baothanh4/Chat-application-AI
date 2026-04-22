package org.example.chatapplication.DTO.Request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ChangePasswordRequest {
    @NotBlank
    @Size(min = 6, max = 128)
    private String oldPassword;

    @NotBlank
    @Size(min = 6, max = 128)
    private String newPassword;

    @NotBlank
    @Size(min = 6, max = 128)
    private String confirmNewPassword;
}
