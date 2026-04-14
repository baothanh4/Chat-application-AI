package org.example.chatapplication.Service;

import lombok.RequiredArgsConstructor;
import org.example.chatapplication.DTO.Response.PresenceResponse;
import org.example.chatapplication.Model.Entity.UserAccount;
import org.example.chatapplication.Repository.UserAccountRepository;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpStatus;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class PresenceService {
    private static final Duration ONLINE_TTL = Duration.ofMinutes(2);
    private static final String KEY_PREFIX = "chat:presence:";
    private static final String LAST_SEEN_FIELD = "lastSeenAt";
    private static final String ONLINE_UNTIL_FIELD = "onlineUntil";

    private final ObjectProvider<@org.jspecify.annotations.NonNull StringRedisTemplate> redisTemplateProvider;
    private final UserAccountRepository userAccountRepository;
    private final Map<UUID, PresenceState> fallbackPresence = new ConcurrentHashMap<>();

    @Transactional
    public void markOnline(UUID userId) {
        Instant now = Instant.now();
        Instant onlineUntil = now.plus(ONLINE_TTL);
        writeRedisPresence(userId, now, onlineUntil);
        touchLastSeenAt(userId, now);
        fallbackPresence.put(userId, new PresenceState(now, onlineUntil, "memory"));
    }

    @Transactional
    public void markOffline(UUID userId) {
        Instant now = Instant.now();
        deleteRedisPresence(userId);
        touchLastSeenAt(userId, now);
        fallbackPresence.remove(userId);
    }

    public PresenceResponse getPresence(UUID userId) {
        Instant now = Instant.now();
        PresenceState state = readRedisPresence(userId).orElseGet(() -> fallbackPresence.get(userId));
        Instant lastSeenAt = state != null ? state.lastSeenAt() : loadLastSeenAt(userId).orElse(null);
        boolean online = state != null && state.onlineUntil() != null && state.onlineUntil().isAfter(now);

        if (!online && state != null && state.onlineUntil() != null && state.onlineUntil().isBefore(now)) {
            fallbackPresence.remove(userId);
        }

        return buildResponse(userId, online, lastSeenAt, state == null ? "db" : state.source());
    }

    public boolean isOnline(UUID userId) {
        return getPresence(userId).isOnline();
    }


    private PresenceResponse buildResponse(UUID userId, boolean online, Instant lastSeenAt, String source) {
        Long minutesSinceLastActive = lastSeenAt == null ? null : Math.max(0L, Duration.between(lastSeenAt, Instant.now()).toMinutes());
        return new PresenceResponse(userId, online, lastSeenAt, minutesSinceLastActive, source);
    }

    private Optional<PresenceState> readRedisPresence(UUID userId) {
        StringRedisTemplate redisTemplate = redisTemplateProvider.getIfAvailable();
        if (redisTemplate != null) {
            try {
                Map<Object, Object> values = redisTemplate.opsForHash().entries(key(userId));
                if (values.isEmpty()) {
                    return Optional.empty();
                }

                Instant lastSeenAt = parseInstant(values.get(LAST_SEEN_FIELD));
                Instant onlineUntil = parseInstant(values.get(ONLINE_UNTIL_FIELD));
                return Optional.of(new PresenceState(lastSeenAt, onlineUntil, "redis"));
            } catch (Exception ignored) {
                // fall back to in-memory presence for local development.
            }
        }
        return Optional.empty();
    }

    private void writeRedisPresence(UUID userId, Instant lastSeenAt, Instant onlineUntil) {
        StringRedisTemplate redisTemplate = redisTemplateProvider.getIfAvailable();
        if (redisTemplate != null) {
            try {
                redisTemplate.opsForHash().put(key(userId), LAST_SEEN_FIELD, lastSeenAt.toString());
                redisTemplate.opsForHash().put(key(userId), ONLINE_UNTIL_FIELD, onlineUntil.toString());
                redisTemplate.expire(key(userId), ONLINE_TTL);
            } catch (Exception ignored) {
                // fall back to in-memory presence for local development.
            }
        }
    }

    private void deleteRedisPresence(UUID userId) {
        StringRedisTemplate redisTemplate = redisTemplateProvider.getIfAvailable();
        if (redisTemplate != null) {
            try {
                redisTemplate.delete(key(userId));
            } catch (Exception ignored) {
                // fall back to in-memory presence for local development.
            }
        }
    }

    private Optional<Instant> loadLastSeenAt(UUID userId) {
        return userAccountRepository.findById(userId).map(UserAccount::getLastSeenAt);
    }

    private void touchLastSeenAt(UUID userId, Instant lastSeenAt) {
        UserAccount user = userAccountRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found: " + userId));
        user.setLastSeenAt(lastSeenAt);
        userAccountRepository.save(user);
    }

    private Instant parseInstant(Object value) {
        if (value == null) {
            return null;
        }

        String text = value.toString().trim();
        if (text.isEmpty()) {
            return null;
        }

        return Instant.parse(text);
    }

    private record PresenceState(Instant lastSeenAt, Instant onlineUntil, String source) {
    }

    private String key(UUID userId) {
        return KEY_PREFIX + userId;
    }
}
