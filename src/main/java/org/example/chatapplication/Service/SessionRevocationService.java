package org.example.chatapplication.Service;

import lombok.RequiredArgsConstructor;
import org.example.chatapplication.Model.Entity.UserSessionRevocation;
import org.example.chatapplication.Repository.UserSessionRevocationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SessionRevocationService {

    private final UserSessionRevocationRepository revocationRepository;

    @Transactional
    public void revokeSessionsForUser(UUID userId) {
        UserSessionRevocation revocation = new UserSessionRevocation(userId, Instant.now());
        revocationRepository.save(revocation);
    }

    @Transactional(readOnly = true)
    public Optional<Instant> getRevokedAt(UUID userId) {
        return revocationRepository.findByUserId(userId).map(UserSessionRevocation::getRevokedAt);
    }
}

