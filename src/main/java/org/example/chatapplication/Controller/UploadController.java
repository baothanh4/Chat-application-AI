package org.example.chatapplication.Controller;

import lombok.RequiredArgsConstructor;
import org.example.chatapplication.DTO.Response.AvatarUploadResponse;
import org.example.chatapplication.DTO.Response.FileUploadResponse;
import org.example.chatapplication.Service.FileStorageService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/uploads")
@RequiredArgsConstructor
public class UploadController {
    private final FileStorageService fileStorageService;

    @PostMapping("/avatar")
    ResponseEntity<AvatarUploadResponse> uploadAvatar(@RequestParam("file") MultipartFile file) {
        String path = fileStorageService.storeAvatar(file);
        return ResponseEntity.status(HttpStatus.CREATED).body(new AvatarUploadResponse(path));
    }

    @PostMapping("/chat-image")
    ResponseEntity<FileUploadResponse> uploadChatImage(@RequestParam("file") MultipartFile file) {
        String url = fileStorageService.storeChatImage(file);
        return ResponseEntity.status(HttpStatus.CREATED).body(new FileUploadResponse(url));
    }

    @PostMapping("/chat-video")
    ResponseEntity<FileUploadResponse> uploadChatVideo(@RequestParam("file") MultipartFile file) {
        String url = fileStorageService.storeChatVideo(file);
        return ResponseEntity.status(HttpStatus.CREATED).body(new FileUploadResponse(url));
    }
}

