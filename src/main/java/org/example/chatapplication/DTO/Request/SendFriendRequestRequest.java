package org.example.chatapplication.DTO.Request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class SendFriendRequestRequest {
    @NotNull
    private UUID senderId;

    @NotNull
    private UUID recipientId;

    @Size(max = 500)
    private String message;
}

