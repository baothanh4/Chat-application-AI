package org.example.chatapplication.DTO.Request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.util.Set;
import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class CreateGroupConversationRequest {
    @NotNull
    UUID ownerId;

    @NotBlank
    @Size(max = 120)
     String name;

    @Size(max = 255)
    String description;

    @Size(max = 500)
    String avatarPath;

    @NotEmpty
    Set<UUID> memberIds;
}
