package org.example.chatapplication.Controller;

import lombok.RequiredArgsConstructor;
import org.example.chatapplication.DTO.Response.RedisImportResponse;
import org.example.chatapplication.Service.RedisImportService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/redis")
@RequiredArgsConstructor
public class RedisController {
    private final RedisImportService redisImportService;

    @PostMapping("/users/import")
    public RedisImportResponse importAllUsers() {
        return redisImportService.importAllUsers();
    }

    @PostMapping("/users/{userId}")
    public RedisImportResponse importSingleUser(@PathVariable UUID userId) {
        return redisImportService.importUser(userId);
    }

    @GetMapping("/users/{userId}")
    public java.util.Map<String, String> readImportedUser(@PathVariable UUID userId) {
        return redisImportService.readUserProfile(userId);
    }
}

