package org.example.chatapplication.DTO.Request;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.example.chatapplication.Model.Enum.MessageType;
import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class SendMessageRequest {
    UUID conversationId;
    @NotNull
    UUID senderId;
    UUID recipientId;
    @NotBlank
    @Size(max = 4000)
    String content;
    MessageType messageType;
    String clientMessageId;

    @AssertTrue(message = "Either conversationId or recipientId must be provided, but not both")
    public boolean isValidTarget() {
        boolean hasConversation = conversationId != null;
        boolean hasRecipient = recipientId != null;
        return hasConversation ^ hasRecipient;
    }
}
