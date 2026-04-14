package org.example.chatapplication.Repository;

import org.example.chatapplication.Model.Entity.DeviceToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DeviceTokenRepository extends JpaRepository<DeviceToken, UUID> {
    List<DeviceToken> findByUserIdAndActiveTrue(UUID userId);

    Optional<DeviceToken> findByToken(String token);
}
