package org.example.chatapplication.DTO.Response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.chatapplication.Model.Enum.UserRole;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AdminUserStatusResponse {
    private UUID userId;
    private String username;
    private UserRole role;
    private boolean accountLocked;
    private String message;
}

