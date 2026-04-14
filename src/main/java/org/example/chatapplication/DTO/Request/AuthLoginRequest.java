package org.example.chatapplication.DTO.Request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AuthLoginRequest {
    @NotBlank
    @Size(max = 80)
    private String username;

    @NotBlank
    @Size(min = 6, max = 72)
    private String password;
}

