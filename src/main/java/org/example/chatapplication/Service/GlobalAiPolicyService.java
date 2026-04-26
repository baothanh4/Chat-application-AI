package org.example.chatapplication.Service;

import lombok.RequiredArgsConstructor;
import org.example.chatapplication.DTO.Request.UpdateGlobalAiPolicyRequest;
import org.example.chatapplication.DTO.Response.GlobalAiPolicyResponse;
import org.example.chatapplication.Model.Entity.GlobalAiPolicy;
import org.example.chatapplication.Repository.GlobalAiPolicyRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class GlobalAiPolicyService {

    private final GlobalAiPolicyRepository globalAiPolicyRepository;

    @Transactional(readOnly = true)
    public GlobalAiPolicy getCurrentPolicy() {
        return globalAiPolicyRepository.findTopByOrderByUpdatedAtDesc()
                .orElseGet(this::defaultPolicy);
    }

    @Transactional(readOnly = true)
    public GlobalAiPolicyResponse getCurrentPolicyResponse() {
        return toResponse(getCurrentPolicy());
    }

    @Transactional
    public GlobalAiPolicyResponse updatePolicy(UpdateGlobalAiPolicyRequest request) {
        GlobalAiPolicy policy = globalAiPolicyRepository.findTopByOrderByUpdatedAtDesc()
                .orElseGet(GlobalAiPolicy::new);

        if (request.getEnabled() != null) {
            policy.setEnabled(request.getEnabled());
        }
        if (request.getSystemPrompt() != null) {
            policy.setSystemPrompt(trimToNull(request.getSystemPrompt()));
        }
        if (request.getProhibitedTopics() != null) {
            policy.setProhibitedTopics(trimToNull(request.getProhibitedTopics()));
        }
        if (request.getMaxTokens() != null) {
            policy.setMaxTokens(request.getMaxTokens());
        }
        if (request.getTemperature() != null) {
            policy.setTemperature(request.getTemperature());
        }

        return toResponse(globalAiPolicyRepository.save(policy));
    }

    public GlobalAiPolicyResponse toResponse(GlobalAiPolicy policy) {
        return new GlobalAiPolicyResponse(
                policy.getId(),
                policy.isEnabled(),
                policy.getSystemPrompt(),
                policy.getProhibitedTopics(),
                policy.getMaxTokens(),
                policy.getTemperature(),
                policy.getUpdatedAt()
        );
    }

    private GlobalAiPolicy defaultPolicy() {
        GlobalAiPolicy policy = new GlobalAiPolicy();
        policy.setEnabled(true);
        policy.setSystemPrompt("Tra loi lich su, ton trong va uu tien su ro rang.");
        policy.setProhibitedTopics(null);
        policy.setMaxTokens(null);
        policy.setTemperature(null);
        return policy;
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}

