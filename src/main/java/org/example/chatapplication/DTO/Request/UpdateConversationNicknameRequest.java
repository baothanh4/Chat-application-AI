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
@NoArgsConstructor
@AllArgsConstructor
public class UpdateConversationNicknameRequest {
    @NotNull
    private UUID actorUserId;

    @Size(max = 120)
    private String nickname;
}

