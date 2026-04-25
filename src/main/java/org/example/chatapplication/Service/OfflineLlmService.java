package org.example.chatapplication.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class OfflineLlmService {

    private final ObjectMapper objectMapper;

    @Value("${chatbot.ollama.enabled:true}")
    private boolean ollamaEnabled;

    @Value("${chatbot.ollama.base-url:http://localhost:11434}")
    private String ollamaBaseUrl;

    @Value("${chatbot.ollama.model:llama3.1:8b}")
    private String ollamaModel;

    @Value("${chatbot.ollama.timeout-seconds:45}")
    private int ollamaTimeoutSeconds;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    public Optional<String> generateWithOllama(String prompt, Double temperature, Integer maxTokens) {
        if (!ollamaEnabled || prompt == null || prompt.isBlank()) {
            return Optional.empty();
        }

        try {
            String endpoint = normalizeBaseUrl(ollamaBaseUrl) + "/api/generate";
            ObjectNode payload = objectMapper.createObjectNode();
            payload.put("model", ollamaModel);
            payload.put("prompt", prompt);
            payload.put("stream", false);

            ObjectNode options = payload.putObject("options");
            if (temperature != null) {
                options.put("temperature", temperature);
            }
            if (maxTokens != null) {
                options.put("num_predict", maxTokens);
            }

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(endpoint))
                    .timeout(Duration.ofSeconds(Math.max(10, ollamaTimeoutSeconds)))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(payload)))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                log.warn("Ollama returned status {}: {}", response.statusCode(), response.body());
                return Optional.empty();
            }

            JsonNode root = objectMapper.readTree(response.body());
            String content = root.path("response").asText("").trim();
            if (content.isBlank()) {
                return Optional.empty();
            }
            return Optional.of(content);
        } catch (Exception ex) {
            log.warn("Ollama local inference failed: {}", ex.getMessage());
            return Optional.empty();
        }
    }

    public String getOllamaModel() {
        return ollamaModel;
    }

    private String normalizeBaseUrl(String url) {
        if (url == null || url.isBlank()) {
            return "http://localhost:11434";
        }
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }
}

