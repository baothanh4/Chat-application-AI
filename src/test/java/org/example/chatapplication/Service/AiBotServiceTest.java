package org.example.chatapplication.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.chatapplication.Repository.ChatMessageRepository;
import org.example.chatapplication.Repository.UserAccountRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class AiBotServiceTest {
    @Mock
    private UserAccountRepository userAccountRepository;
    @Mock
    private ConversationAiService conversationAiService;
    @Mock
    private ChatMessageRepository chatMessageRepository;

    private AiBotService aiBotService;

    @BeforeEach
    void setUp() {
        aiBotService = new AiBotService(
                userAccountRepository,
                conversationAiService,
                chatMessageRepository,
                new ObjectMapper()
        );
        ReflectionTestUtils.setField(aiBotService, "botUsername", "ai-assistant");
        ReflectionTestUtils.setField(aiBotService, "openRouterApiKey", "");
        ReflectionTestUtils.setField(aiBotService, "openRouterModel", "google/gemini-2.0-flash-001");
    }

    @Test
    void generateBotReplyShouldAnswerVietnamTimeQuestionsDirectly() {
        String reply = aiBotService.generateBotReply("bây giờ là mấy giờ ở Việt Nam", UUID.randomUUID());

        assertThat(reply).contains("Việt Nam");
        assertThat(reply).containsPattern("\\d{2}:\\d{2}, \\d{2}/\\d{2}/\\d{4}");
    }

    @Test
    void generateBotReplyShouldSupportGeneralQuestionsInFallbackMode() {
        String reply = aiBotService.generateBotReply("Bạn có thể làm gì?", UUID.randomUUID());

        assertThat(reply).contains("kiến thức chung");
        assertThat(reply).contains("task");
        assertThat(reply).doesNotContain("chỉ riêng tóm tắt");
    }

    @Test
    void helpMessageShouldDescribeGeneralAssistantCapabilities() {
        String reply = aiBotService.generateBotReply("help", UUID.randomUUID());

        assertThat(reply).contains("hỏi tự do");
        assertThat(reply).contains("kiến thức chung");
        assertThat(reply).contains("thời gian");
    }
}

