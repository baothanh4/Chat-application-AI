package org.example.chatapplication.Controller;


import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.chatapplication.DTO.Request.RegisterDeviceTokenRequest;
import org.example.chatapplication.Model.Entity.DeviceToken;
import org.example.chatapplication.Model.Entity.UserAccount;
import org.example.chatapplication.Repository.DeviceTokenRepository;
import org.example.chatapplication.Service.UserAccountService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/device-tokens")
@RequiredArgsConstructor
@Validated
public class DeviceTokenController {
    private final DeviceTokenRepository deviceTokenRepository;
    private final UserAccountService userAccountService;

    @PostMapping
    ResponseEntity<Void> register(@RequestBody @Valid RegisterDeviceTokenRequest request) {
        UserAccount user = userAccountService.requireUser(request.getUserId());
        DeviceToken token = deviceTokenRepository.findByToken(request.getToken().trim()).orElseGet(DeviceToken::new);
        token.setUser(user);
        token.setToken(request.getToken().trim());
        token.setPlatform(request.getPlatform());
        token.setActive(true);
        deviceTokenRepository.save(token);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }
}
