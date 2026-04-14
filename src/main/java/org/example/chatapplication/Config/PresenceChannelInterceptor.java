package org.example.chatapplication.Config;

import lombok.RequiredArgsConstructor;
import org.example.chatapplication.Service.PresenceService;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.Map;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class PresenceChannelInterceptor implements ChannelInterceptor {
    private static final String SESSION_USER_ID_KEY = "chat.userId";
    private static final String USER_ID_HEADER = "userId";

    private final PresenceService presenceService;

    @Override
    public Message<?> preSend(@NonNull Message<?> message, @NonNull MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null) {
            return message;
        }

        StompCommand command = accessor.getCommand();
        if (command == null) {
            return message;
        }

        switch (command) {
            case CONNECT -> handleConnect(accessor);
            case SEND -> handleSend(accessor);
            case DISCONNECT -> handleDisconnect(accessor);
            default -> {
            }
        }

        return message;
    }

    private void handleConnect(StompHeaderAccessor accessor) {
        UUID userId = resolveUserId(accessor);
        if (userId == null) {
            return;
        }

        Map<String, Object> sessionAttributes = accessor.getSessionAttributes();
        if (sessionAttributes != null) {
            sessionAttributes.put(SESSION_USER_ID_KEY, userId);
        }

        presenceService.markOnline(userId);
    }

    private void handleSend(StompHeaderAccessor accessor) {
        UUID userId = resolveSessionUserId(accessor);
        if (userId != null) {
            presenceService.markOnline(userId);
        }
    }

    private void handleDisconnect(StompHeaderAccessor accessor) {
        UUID userId = resolveSessionUserId(accessor);
        if (userId != null) {
            presenceService.markOffline(userId);
        }
    }

    @Nullable
    private UUID resolveSessionUserId(StompHeaderAccessor accessor) {
        Map<String, Object> sessionAttributes = accessor.getSessionAttributes();
        if (sessionAttributes == null) {
            return null;
        }

        Object value = sessionAttributes.get(SESSION_USER_ID_KEY);
        return value instanceof UUID uuid ? uuid : parseUuid(value == null ? null : value.toString());
    }

    @Nullable
    private UUID resolveUserId(StompHeaderAccessor accessor) {
        String header = accessor.getFirstNativeHeader(USER_ID_HEADER);
        if (!StringUtils.hasText(header)) {
            return resolveSessionUserId(accessor);
        }
        return parseUuid(header);
    }

    @Nullable
    private UUID parseUuid(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }

        try {
            return UUID.fromString(value.trim());
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }
}

