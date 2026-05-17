package org.example.chatapplication.Exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorCode {
    INVALID_INPUT(1001, "Invalid input", HttpStatus.BAD_REQUEST),
    INVALID_CREDENTIALS(1002, "Invalid credentials", HttpStatus.UNAUTHORIZED),
    FORBIDDEN(1003, "Forbidden access", HttpStatus.FORBIDDEN),
    NOT_A_MEMBER(1004, "Not a member of this conversation", HttpStatus.FORBIDDEN),
    CONVERSATION_NOT_FOUND(1005, "Conversation not found", HttpStatus.NOT_FOUND),
    USER_NOT_FOUND(1006, "User not found", HttpStatus.NOT_FOUND),
    USER_ALREADY_EXISTS(1007, "User already exists", HttpStatus.CONFLICT),
    ACCOUNT_LOCKED(1008, "Account is locked", HttpStatus.LOCKED),
    UNCATEGORIZED_EXCEPTION(9999, "Uncategorized error", HttpStatus.INTERNAL_SERVER_ERROR);

    private final int code;
    private final String message;
    private final HttpStatus statusCode;

    ErrorCode(int code, String message, HttpStatus statusCode) {
        this.code = code;
        this.message = message;
        this.statusCode = statusCode;
    }
}

