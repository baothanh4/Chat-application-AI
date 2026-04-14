package org.example.chatapplication.Controller;

import org.example.chatapplication.DTO.Response.UserResponse;
import lombok.Getter;

import java.util.List;

@Getter
public class FaceEnrollmentConflictException extends RuntimeException {
    private final List<UserResponse> existingUsers;

    public FaceEnrollmentConflictException(String message, List<UserResponse> existingUsers) {
        super(message);
        this.existingUsers = existingUsers;
    }

}

