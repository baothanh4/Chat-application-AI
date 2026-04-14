package org.example.chatapplication.Controller;


import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.example.chatapplication.DTO.Request.CreateUserRequest;
import org.example.chatapplication.DTO.Response.UserResponse;
import org.example.chatapplication.DTO.Response.UserSearchResponse;
import org.example.chatapplication.Model.Entity.UserAccount;
import org.example.chatapplication.Service.UserDirectoryService;
import org.example.chatapplication.Service.UserAccountService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Validated
public class UserController {
    private final UserAccountService userAccountService;
    private final UserDirectoryService userDirectoryService;

    @PostMapping
    public ResponseEntity<UserResponse> createUser(@RequestBody @Valid CreateUserRequest request) {
        UserAccount user = userAccountService.createOrUpdate(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(userAccountService.toResponse(user));
    }

    @GetMapping("/{userId}")
    public ResponseEntity<UserResponse> getUser(@PathVariable UUID userId) {
        return ResponseEntity.ok(userAccountService.toResponse(userAccountService.requireUser(userId)));
    }

    @PutMapping("/{userId}")
    public ResponseEntity<UserResponse> updateUser(@PathVariable UUID userId, @RequestBody @Valid org.example.chatapplication.DTO.Request.UpdateUserRequest request) {
        UserAccount user = userAccountService.updateProfile(userId, request);
        return ResponseEntity.ok(userAccountService.toResponse(user));
    }

    @PostMapping("/login")
    public ResponseEntity<UserResponse> loginUser(@RequestBody @Valid org.example.chatapplication.DTO.Request.LoginRequest request) {
        UserAccount user = userAccountService.authenticate(request.getUsername(), request.getPassword());
        return ResponseEntity.ok(userAccountService.toResponse(user));
    }

    @PostMapping("/{userId}/avatar")
    public ResponseEntity<UserResponse> uploadAvatar(@PathVariable UUID userId, @RequestParam("file") org.springframework.web.multipart.MultipartFile file) {
        UserAccount user = userAccountService.uploadAvatar(userId, file);
        return ResponseEntity.ok(userAccountService.toResponse(user));
    }

    @GetMapping("/search")
    public ResponseEntity<List<UserSearchResponse>> searchUsers(@RequestParam String query,
                                                                @RequestParam(required = false) UUID viewerId,
                                                                @RequestParam(defaultValue = "10") int limit) {
        return ResponseEntity.ok(userDirectoryService.searchSuggestions(query, limit, viewerId));
    }
}
