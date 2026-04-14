package org.example.chatapplication.DTO.Request;

import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CreatePrivateConversationRequest {
    @NotNull
     UUID ownerId;
    @NotNull
     UUID recipientId;
}
