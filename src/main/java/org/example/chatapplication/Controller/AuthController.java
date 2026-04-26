package org.example.chatapplication.Controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.chatapplication.DTO.Request.AuthLoginRequest;
import org.example.chatapplication.DTO.Request.AuthRegisterRequest;
import org.example.chatapplication.DTO.Response.AuthResponse;
import org.example.chatapplication.DTO.Response.UserResponse;
import org.example.chatapplication.Service.AuthService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;

    @PostMapping(value = "/register", consumes = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<UserResponse> register(@RequestBody @Valid AuthRegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(request));
    }

    @PostMapping(value = "/register/face", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    ResponseEntity<UserResponse> registerWithFace(@ModelAttribute @Valid AuthRegisterRequest request, @RequestParam("faceImage") MultipartFile faceImage) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.registerWithFace(request, faceImage));
    }

    @PostMapping(value = "/login", consumes = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<AuthResponse> login(@RequestBody @Valid AuthLoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping(value = "/login/face", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    ResponseEntity<AuthResponse> loginWithFace(@RequestParam("faceImage") MultipartFile faceImage) {
        return ResponseEntity.ok(authService.loginWithFace(faceImage));
    }

    @PostMapping(value = "/login/face/confirm", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    ResponseEntity<AuthResponse> confirmFaceLogin(@RequestParam("faceImage") MultipartFile faceImage,
                                                  @RequestParam("selectedUserId") java.util.UUID selectedUserId) {
        return ResponseEntity.ok(authService.confirmFaceLogin(faceImage, selectedUserId));
    }
}

