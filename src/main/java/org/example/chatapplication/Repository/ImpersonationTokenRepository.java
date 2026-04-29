package org.example.chatapplication.Repository;

import org.example.chatapplication.Model.Entity.ImpersonationToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ImpersonationTokenRepository extends JpaRepository<ImpersonationToken, UUID> {
    Optional<ImpersonationToken> findByToken(String token);
}

