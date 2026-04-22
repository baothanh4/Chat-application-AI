package org.example.chatapplication.Model.Entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "chat_users")
public class UserAccount extends BaseEntity{
    @Column(nullable = false, unique = true, length = 80)
    private String username;

    @Column(nullable = false, length = 120)
    private String displayName;

    @Column(length = 120)
    private String fullName;

    @Column(length = 20)
    private String phoneNumber;

    @Column
    private String address;

    @Column(length = 500)
    private String bio;

    @Column
    private LocalDate dateOfBirth;

    @Column(length = 20)
    private String gender;

    @JsonIgnore
    @Column(name = "password_hash")
    private String passwordHash;

    @Column(length = 160)
    private String email;

    @Column(name = "avatar_path")
    private String avatarPath;

    @Column(name = "face_template_path", length = 255)
    private String faceTemplatePath;

    @Column(name = "face_template_hash", length = 128)
    private String faceTemplateHash;

    @Column(name = "face_enrolled_at")
    private Instant faceEnrolledAt;

    @Column(name = "face_login_enabled", nullable = false)
    private Boolean faceLoginEnabled = Boolean.FALSE;

    @Column(name = "last_seen_at")
    private Instant lastSeenAt;

    public boolean isFaceLoginEnabled() {
        return Boolean.TRUE.equals(faceLoginEnabled);
    }

    public void setFaceLoginEnabled(Boolean faceLoginEnabled) {
        this.faceLoginEnabled = Boolean.TRUE.equals(faceLoginEnabled);
    }
}
