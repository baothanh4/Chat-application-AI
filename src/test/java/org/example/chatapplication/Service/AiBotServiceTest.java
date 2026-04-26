package org.example.chatapplication.Service;

import org.example.chatapplication.Repository.ChatMessageRepository;
import org.example.chatapplication.Repository.UserAccountRepository;
import org.example.chatapplication.Model.Entity.GlobalAiPolicy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.UUID;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AiBotServiceTest {
    @Mock
    private UserAccountRepository userAccountRepository;
    @Mock
    private ConversationAiService conversationAiService;
    @Mock
    private ChatMessageRepository chatMessageRepository;
    @Mock
    private ConversationService conversationService;
    @Mock
    private OfflineLlmService offlineLlmService;
    @Mock
    private GlobalAiPolicyService globalAiPolicyService;

    private AiBotService aiBotService;

    @BeforeEach
    void setUp() {
        aiBotService = new AiBotService(
                userAccountRepository,
                conversationAiService,
                conversationService,
                chatMessageRepository,
                offlineLlmService,
                globalAiPolicyService
        );
        when(globalAiPolicyService.getCurrentPolicy()).thenReturn(defaultGlobalPolicy());
        ReflectionTestUtils.setField(aiBotService, "botUsername", "ai-assistant");
        ReflectionTestUtils.setField(aiBotService, "localAiEnabled", true);
        ReflectionTestUtils.setField(aiBotService, "defaultTemperature", 0.7d);
        ReflectionTestUtils.setField(aiBotService, "defaultMaxTokens", 512);
    }

    @Test
    void generateBotReplyShouldAnswerVietnamTimeQuestionsDirectly() {
        String reply = aiBotService.generateBotReply("bây giờ là mấy giờ ở Việt Nam", UUID.randomUUID());

        assertThat(reply).contains("Viet Nam");
        assertThat(reply).containsPattern("\\d{2}:\\d{2}, \\d{2}/\\d{2}/\\d{4}");
    }

    @Test
    void generateBotReplyShouldSupportGeneralQuestionsInFallbackMode() {
        when(offlineLlmService.generateWithOllama(org.mockito.ArgumentMatchers.anyString(), anyDouble(), anyInt()))
                .thenReturn(Optional.empty());

        String reply = aiBotService.generateBotReply("Bạn có thể làm gì?", UUID.randomUUID());

        assertThat(reply).contains("model offline");
        assertThat(reply).contains("thu lai");
    }

    @Test
    void helpMessageShouldDescribeGeneralAssistantCapabilities() {
        when(offlineLlmService.getOllamaModel()).thenReturn("llama3.1:8b");

        String reply = aiBotService.generateBotReply("help", UUID.randomUUID());

        assertThat(reply).contains("Offline Model");
        assertThat(reply).contains("tom tat");
        assertThat(reply).contains("llama3.1:8b");
    }

    @Test
    void generateBotReplyShouldUseOfflineModelWhenAvailable() {
        when(offlineLlmService.generateWithOllama(org.mockito.ArgumentMatchers.anyString(), anyDouble(), anyInt()))
                .thenReturn(Optional.of("Tra loi tu model offline"));

        String reply = aiBotService.generateBotReply("Giai thich clean architecture", UUID.randomUUID());

        assertThat(reply).isEqualTo("Tra loi tu model offline");
    }

    private GlobalAiPolicy defaultGlobalPolicy() {
        GlobalAiPolicy policy = new GlobalAiPolicy();
        policy.setEnabled(true);
        return policy;
    }
}

