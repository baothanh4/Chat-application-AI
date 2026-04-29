package org.example.chatapplication.Repository;

import org.example.chatapplication.Model.Entity.UserSessionRevocation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface UserSessionRevocationRepository extends JpaRepository<UserSessionRevocation, UUID> {
    Optional<UserSessionRevocation> findByUserId(UUID userId);
}

