package org.example.chatapplication.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.chatapplication.Model.Entity.ChatMessage;
import org.example.chatapplication.Model.Entity.Conversation;
import org.example.chatapplication.Model.Entity.DeviceToken;
import org.example.chatapplication.Model.Entity.UserAccount;
import org.example.chatapplication.Model.Enum.MessageType;
import org.example.chatapplication.Repository.DeviceTokenRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {
    private final DeviceTokenRepository deviceTokenRepository;

    public void sendOfflineMessageNotification(UserAccount recipient, ChatMessage message, Conversation conversation) {
        List<DeviceToken> tokens = deviceTokenRepository.findByUserIdAndActiveTrue(recipient.getId());
        String payload = String.format("New message from %s in %s: %s",
                message.getSender().getDisplayName(),
                conversation.getName(),
                toNotificationPreview(message));

        if (tokens.isEmpty()) {
            log.info("Push notification skipped for {} because no active device tokens were found. Payload={}", recipient.getUsername(), payload);
            return;
        }

        for (DeviceToken token : tokens) {
            log.info("Push notification ready for {} via {} token {} | payload={}",
                    recipient.getUsername(), token.getPlatform(), maskToken(token.getToken()), payload);
        }
    }

    private String maskToken(String token) {
        if (token == null || token.length() <= 8) {
            return token;
        }
        return token.substring(0, 4) + "..." + token.substring(token.length() - 4);
    }

    private String toNotificationPreview(ChatMessage message) {
        if (message.getMessageType() == MessageType.IMAGE) {
            return "[Image]";
        }
        if (message.getMessageType() == MessageType.VIDEO) {
            return "[Video]";
        }
        return message.getContent();
    }
}
