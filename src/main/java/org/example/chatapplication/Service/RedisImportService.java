package org.example.chatapplication.Service;

import lombok.RequiredArgsConstructor;
import org.example.chatapplication.DTO.Response.RedisImportResponse;
import org.example.chatapplication.Model.Entity.UserAccount;
import org.example.chatapplication.Repository.UserAccountRepository;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RedisImportService {
    private static final String USER_PROFILE_KEY_PREFIX = "chat:user:profile:";
    private static final String SOURCE = "postgresql";

    private final UserAccountRepository userAccountRepository;
    private final StringRedisTemplate redisTemplate;

    @Transactional(readOnly = true)
    public RedisImportResponse importAllUsers() {
        return importUsers(userAccountRepository.findAll());
    }

    @Transactional(readOnly = true)
    public RedisImportResponse importUser(UUID userId) {
        UserAccount user = userAccountRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found: " + userId));
        return importUsers(List.of(user));
    }

    @Transactional(readOnly = true)
    public Map<String, String> readUserProfile(UUID userId) {
        Map<Object, Object> entries = redisTemplate.opsForHash().entries(key(userId));
        Map<String, String> values = new LinkedHashMap<>();
        for (Map.Entry<Object, Object> entry : entries.entrySet()) {
            if (entry.getKey() != null && entry.getValue() != null) {
                values.put(String.valueOf(entry.getKey()), String.valueOf(entry.getValue()));
            }
        }
        return values;
    }

    private RedisImportResponse importUsers(Collection<UserAccount> users) {
        int importedCount = 0;
        for (UserAccount user : users) {
            redisTemplate.opsForHash().putAll(key(user.getId()), toRedisHash(user));
            importedCount++;
        }
        return new RedisImportResponse(importedCount, users.size(), USER_PROFILE_KEY_PREFIX, SOURCE);
    }

    private Map<String, String> toRedisHash(UserAccount user) {
        Map<String, String> values = new LinkedHashMap<>();
        putIfNotBlank(values, "id", String.valueOf(user.getId()));
        putIfNotBlank(values, "username", user.getUsername());
        putIfNotBlank(values, "displayName", user.getDisplayName());
        putIfNotBlank(values, "fullName", user.getFullName());
        putIfNotBlank(values, "phoneNumber", user.getPhoneNumber());
        putIfNotBlank(values, "address", user.getAddress());
        putIfNotBlank(values, "bio", user.getBio());
        putIfNotBlank(values, "dateOfBirth", user.getDateOfBirth() == null ? null : user.getDateOfBirth().format(DateTimeFormatter.ISO_LOCAL_DATE));
        putIfNotBlank(values, "gender", user.getGender());
        putIfNotBlank(values, "email", user.getEmail());
        putIfNotBlank(values, "avatarPath", user.getAvatarPath());
        putIfNotBlank(values, "createdAt", user.getCreatedAt() == null ? null : user.getCreatedAt().toString());
        putIfNotBlank(values, "updatedAt", user.getUpdatedAt() == null ? null : user.getUpdatedAt().toString());
        values.put("source", "postgresql");
        return values;
    }

    private void putIfNotBlank(Map<String, String> values, String key, String value) {
        if (value != null && !value.trim().isEmpty()) {
            values.put(key, value.trim());
        }
    }


    private String key(UUID userId) {
        return USER_PROFILE_KEY_PREFIX + userId;
    }
}

