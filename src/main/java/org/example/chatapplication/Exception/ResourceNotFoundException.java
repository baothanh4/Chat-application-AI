package org.example.chatapplication.Exception;

public class ResourceNotFoundException extends AppException {
    public ResourceNotFoundException(ErrorCode errorCode) {
        super(errorCode);
    }

    public ResourceNotFoundException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }

    public static ResourceNotFoundException userNotFound() {
        return new ResourceNotFoundException(ErrorCode.USER_NOT_FOUND);
    }
}
