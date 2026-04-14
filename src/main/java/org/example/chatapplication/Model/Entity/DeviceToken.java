package org.example.chatapplication.Model.Entity;


import jakarta.persistence.*;
import lombok.*;
import org.example.chatapplication.Model.Enum.PushPlatform;
import org.example.chatapplication.Model.Entity.UserAccount;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(
        name = "device_tokens",
        uniqueConstraints = @UniqueConstraint(name = "uk_device_token", columnNames = {"token"})
)
public class DeviceToken extends BaseEntity{
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    @ToString.Exclude
    private UserAccount user;

    @Column(nullable = false, length = 512)
    private String token;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PushPlatform platform = PushPlatform.WEB;

    @Column(nullable = false)
    private boolean active = true;
}
