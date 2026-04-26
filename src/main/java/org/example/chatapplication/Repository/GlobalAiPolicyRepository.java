package org.example.chatapplication.Repository;

import org.example.chatapplication.Model.Entity.GlobalAiPolicy;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface GlobalAiPolicyRepository extends JpaRepository<GlobalAiPolicy, UUID> {
    Optional<GlobalAiPolicy> findTopByOrderByUpdatedAtDesc();
}

