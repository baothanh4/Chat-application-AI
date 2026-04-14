package org.example.chatapplication.Config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.example.chatapplication.Service.PasswordHasher;

@Configuration
public class AuthConfig {
    @Bean
    PasswordHasher passwordHasher() {
        return new PasswordHasher();
    }
}

