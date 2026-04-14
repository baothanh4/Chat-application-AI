package org.example.chatapplication.DTO.Request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.example.chatapplication.Model.Enum.PushPlatform;


import java.util.UUID;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class RegisterDeviceTokenRequest {
    @NotNull
    UUID userId;
    @NotBlank
    @Size(max = 512)
    String token;
    @NotNull
    PushPlatform platform;
}
