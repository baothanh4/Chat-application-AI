package org.example.chatapplication.Exception;

import org.example.chatapplication.DTO.Response.FaceLoginCandidateResponse;
import lombok.Getter;

import java.util.List;

@Getter
public class FaceLoginAmbiguousException extends RuntimeException {
    private final List<FaceLoginCandidateResponse> candidates;
    private final int threshold;
    private final int ambiguityMargin;

    public FaceLoginAmbiguousException(String message, List<FaceLoginCandidateResponse> candidates, int threshold, int ambiguityMargin) {
        super(message);
        this.candidates = candidates;
        this.threshold = threshold;
        this.ambiguityMargin = ambiguityMargin;
    }
}

