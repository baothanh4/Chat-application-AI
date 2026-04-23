package org.example.chatapplication.Model.Entity;

import jakarta.persistence.*;
import lombok.*;

import org.example.chatapplication.Model.Enum.ConversationType;

import java.util.LinkedHashSet;
import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "chat_conversations")
public class Conversation extends BaseEntity {

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ConversationType type;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(length = 255)
    private String description;

    @Column(nullable = false)
    private boolean archived = false;

    @Column(length = 500)
    private String avatarPath;

    @OneToMany(mappedBy = "conversation", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @ToString.Exclude
    private Set<ConversationMember> members = new LinkedHashSet<>();
}
