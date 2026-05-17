package org.example.chatapplication.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AiInferenceService {

    private final ChatModel chatModel;

    @Value("${spring.ai.openai.api-key:}")
    private String openRouterApiKey;

    public Optional<String> generate(String prompt, Double temperature, Integer maxTokens) {
        // Prefer OpenRouter if API key is present
        if (openRouterApiKey != null && !openRouterApiKey.isBlank()) {
            try {
                log.info("Generating response using OpenRouter (model: ring-2.6-1t)...");
                org.springframework.ai.chat.prompt.ChatOptions options = org.springframework.ai.openai.OpenAiChatOptions.builder()
                        .temperature(temperature != null ? temperature : 0.7)
                        .maxTokens(maxTokens != null ? maxTokens : 1024)
                        .build();
                
                ChatResponse response = chatModel.call(new Prompt(prompt, options));
                String content = response.getResult().getOutput().getText();
                if (content != null && !content.isBlank()) {
                    return Optional.of(content.trim());
                }
            } catch (Exception ex) {
                log.error("OpenRouter generation failed: {}.", ex.getMessage());
            }
        }
        
        return Optional.empty();
    }

    public String getActiveModelName() {
        return "OpenRouter (ring-2.6-1t)";
    }
}
