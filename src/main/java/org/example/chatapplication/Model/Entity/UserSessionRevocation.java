package org.example.chatapplication.Model.Entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "user_session_revocations")
public class UserSessionRevocation {

    @Id
    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "revoked_at", nullable = false)
    private Instant revokedAt;

    public UserSessionRevocation(UUID userId, Instant revokedAt) {
        this.userId = userId;
        this.revokedAt = revokedAt;
    }
}

