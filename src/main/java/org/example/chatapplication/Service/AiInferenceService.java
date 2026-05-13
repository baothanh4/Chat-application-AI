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
    private final OfflineLlmService offlineLlmService;

    @Value("${spring.ai.openai.api-key:}")
    private String openRouterApiKey;

    public Optional<String> generate(String prompt, Double temperature, Integer maxTokens) {
        // Prefer OpenRouter if API key is present
        if (openRouterApiKey != null && !openRouterApiKey.isBlank()) {
            try {
                log.info("Generating response using OpenRouter...");
                ChatResponse response = chatModel.call(new Prompt(prompt));
                String content = response.getResult().getOutput().getText();
                if (content != null && !content.isBlank()) {
                    return Optional.of(content.trim());
                }
            } catch (Exception ex) {
                log.error("OpenRouter generation failed: {}", ex.getMessage());
            }
        }

        // Fallback to Ollama
        log.info("Falling back to Ollama for generation...");
        return offlineLlmService.generateWithOllama(prompt, temperature, maxTokens);
    }

    public String getActiveModelName() {
        if (openRouterApiKey != null && !openRouterApiKey.isBlank()) {
            return "OpenRouter (ring-2.6-1t)";
        }
        return "Ollama (" + offlineLlmService.getOllamaModel() + ")";
    }
}
