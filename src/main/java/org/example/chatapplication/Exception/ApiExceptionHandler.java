package org.example.chatapplication.Exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class ApiExceptionHandler {
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleBadRequest(IllegalArgumentException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", ex.getMessage()));
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, String>> handleServerError(IllegalStateException ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("message", ex.getMessage()));
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, String>> handleResponseStatus(ResponseStatusException ex) {
        String message = ex.getReason() == null ? ex.getMessage() : ex.getReason();
        return ResponseEntity.status(ex.getStatusCode()).body(Map.of("message", message));
    }

    @ExceptionHandler(FaceEnrollmentConflictException.class)
    public ResponseEntity<Map<String, Object>> handleFaceEnrollmentConflict(FaceEnrollmentConflictException ex) {
        Map<String, Object> body = new HashMap<>();
        body.put("message", ex.getMessage());
        body.put("existingUsers", ex.getExistingUsers());
        body.put("count", ex.getExistingUsers().size());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(body);
    }

    @ExceptionHandler(FaceLoginAmbiguousException.class)
    public ResponseEntity<Map<String, Object>> handleFaceLoginAmbiguous(FaceLoginAmbiguousException ex) {
        Map<String, Object> body = new HashMap<>();
        body.put("message", ex.getMessage());
        body.put("code", "FACE_LOGIN_AMBIGUOUS");
        body.put("candidates", ex.getCandidates());
        body.put("count", ex.getCandidates().size());
        body.put("threshold", ex.getThreshold());
        body.put("ambiguityMargin", ex.getAmbiguityMargin());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(body);
    }
}

