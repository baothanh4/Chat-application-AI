package org.example.chatapplication.Model.Entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "global_ai_policy")
public class GlobalAiPolicy extends BaseEntity {

    @Column(nullable = false)
    private Boolean enabled = Boolean.TRUE;

    @Column(name = "system_prompt", length = 4000)
    private String systemPrompt;

    @Column(name = "prohibited_topics", length = 2000)
    private String prohibitedTopics;

    @Column(name = "max_tokens")
    private Integer maxTokens;

    @Column(name = "temperature")
    private Double temperature;

    public boolean isEnabled() {
        return Boolean.TRUE.equals(enabled);
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = Boolean.TRUE.equals(enabled);
    }
}

