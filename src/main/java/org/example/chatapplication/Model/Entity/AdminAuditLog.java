package org.example.chatapplication.Model.Entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.example.chatapplication.Model.Enum.AdminAuditAction;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "admin_audit_logs")
public class AdminAuditLog extends BaseEntity {

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private AdminAuditAction action;

    @Column(name = "actor_username", nullable = false, length = 120)
    private String actorUsername;

    @Column(name = "target_user_id")
    private UUID targetUserId;

    @Column(length = 2000)
    private String details;
}

