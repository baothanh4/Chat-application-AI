package org.example.chatapplication.Service;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;
import java.util.UUID;

@Service
public class FileStorageService {
    private static final Path AVATAR_DIR = Paths.get("uploads", "avatars");
    private static final Path CHAT_IMAGE_DIR = Paths.get("uploads", "chat-images");
    private static final Path CHAT_VIDEO_DIR = Paths.get("uploads", "chat-videos");
    private static final Path CHAT_FILE_DIR = Paths.get("uploads", "chat-files");
    private static final Path FACE_TEMPLATE_DIR = Paths.get("uploads", "face-templates");

    public String storeAvatar(MultipartFile file) {
        validateImage(file);
        try {
            Files.createDirectories(AVATAR_DIR);
            String extension = getExtension(file.getOriginalFilename());
            String filename = UUID.randomUUID() + extension;
            Path target = AVATAR_DIR.resolve(filename).normalize();
            try (InputStream inputStream = file.getInputStream()) {
                Files.copy(inputStream, target);
            }
            return "/uploads/avatars/" + filename;
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to store avatar file", ex);
        }
    }

    public String storeChatImage(MultipartFile file) {
        validateImage(file);
        try {
            Files.createDirectories(CHAT_IMAGE_DIR);
            String extension = getExtension(file.getOriginalFilename());
            String filename = UUID.randomUUID() + extension;
            Path target = CHAT_IMAGE_DIR.resolve(filename).normalize();
            try (InputStream inputStream = file.getInputStream()) {
                Files.copy(inputStream, target);
            }
            return "/uploads/chat-images/" + filename;
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to store chat image file", ex);
        }
    }

    public String storeChatVideo(MultipartFile file) {
        validateVideo(file);
        try {
            Files.createDirectories(CHAT_VIDEO_DIR);
            String extension = getExtension(file.getOriginalFilename());
            String filename = UUID.randomUUID() + extension;
            Path target = CHAT_VIDEO_DIR.resolve(filename).normalize();
            try (InputStream inputStream = file.getInputStream()) {
                Files.copy(inputStream, target);
            }
            return "/uploads/chat-videos/" + filename;
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to store chat video file", ex);
        }
    }

    public String storeChatFile(MultipartFile file) {
        validateFile(file);
        try {
            Files.createDirectories(CHAT_FILE_DIR);
            String extension = getExtension(file.getOriginalFilename());
            String filename = UUID.randomUUID() + extension;
            Path target = CHAT_FILE_DIR.resolve(filename).normalize();
            try (InputStream inputStream = file.getInputStream()) {
                Files.copy(inputStream, target);
            }
            return "/uploads/chat-files/" + filename;
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to store chat file", ex);
        }
    }

    public String storeFaceTemplate(MultipartFile file) {
        validateImage(file);
        try {
            Files.createDirectories(FACE_TEMPLATE_DIR);
            String extension = getExtension(file.getOriginalFilename());
            String filename = UUID.randomUUID() + extension;
            Path target = FACE_TEMPLATE_DIR.resolve(filename).normalize();
            try (InputStream inputStream = file.getInputStream()) {
                Files.copy(inputStream, target);
            }
            return "/uploads/face-templates/" + filename;
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to store face template file", ex);
        }
    }

    public void deleteStoredFile(String storedPath) {
        if (!StringUtils.hasText(storedPath)) {
            return;
        }

        String normalizedPath = storedPath.startsWith("/") ? storedPath.substring(1) : storedPath;
        Path filePath = Paths.get(normalizedPath).normalize();
        try {
            Files.deleteIfExists(filePath);
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to delete stored file", ex);
        }
    }

    private void validateImage(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Image file is required");
        }
        String contentType = file.getContentType();
        if (isAllowedContentType(contentType, "image/") || hasAllowedExtension(file, ".jpg", ".jpeg", ".png", ".gif", ".webp", ".bmp", ".avif", ".jfif")) {
            return;
        }
        throw new IllegalArgumentException("Only image files are allowed");
    }

    private void validateVideo(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Video file is required");
        }
        String contentType = file.getContentType();
        if (isAllowedContentType(contentType, "video/") || hasAllowedExtension(file, ".mp4", ".mov", ".webm", ".mkv", ".avi", ".m4v")) {
            return;
        }
        throw new IllegalArgumentException("Only video files are allowed");
    }

    private boolean isAllowedContentType(String contentType, String prefix) {
        return contentType != null && contentType.toLowerCase(Locale.ROOT).startsWith(prefix);
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File is required");
        }
    }

    private boolean hasAllowedExtension(MultipartFile file, String... extensions) {
        String extension = getExtension(file.getOriginalFilename());
        if (!StringUtils.hasText(extension)) {
            return false;
        }
        for (String allowed : extensions) {
            if (allowed.equalsIgnoreCase(extension)) {
                return true;
            }
        }
        return false;
    }

    private String getExtension(String originalFilename) {
        if (!StringUtils.hasText(originalFilename)) {
            return "";
        }
        String filename = originalFilename.toLowerCase(Locale.ROOT);
        int dotIndex = filename.lastIndexOf('.');
        if (dotIndex < 0) {
            return "";
        }
        return filename.substring(dotIndex);
    }
}

