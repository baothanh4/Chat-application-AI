package org.example.chatapplication.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.chatapplication.DTO.Response.ConversationAiInsightResponse;
import org.example.chatapplication.DTO.Response.ConversationAiTaskResponse;
import org.example.chatapplication.Model.Entity.ChatMessage;
import org.example.chatapplication.Model.Entity.UserAccount;
import org.example.chatapplication.Repository.ChatMessageRepository;
import org.example.chatapplication.Repository.UserAccountRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

@Service
@Slf4j
public class AiBotService {

    private static final Duration OPENROUTER_COOLDOWN = Duration.ofMinutes(2);
    private static final ZoneId VIETNAM_ZONE_ID = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final DateTimeFormatter VIETNAM_TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm, dd/MM/yyyy");

    private final UserAccountRepository userAccountRepository;
    private final ConversationAiService conversationAiService;
    private final ChatMessageRepository chatMessageRepository;
    private final ObjectMapper objectMapper;
    private final ConcurrentMap<UUID, Instant> openRouterCooldownUntil = new ConcurrentHashMap<>();

    @Value("${chatbot.bot-username:ai-assistant}")
    private String botUsername;

    @Value("${openrouter.api-key:}")
    private String openRouterApiKey;

    @Value("${openrouter.model:google/gemini-2.0-flash-001}")
    private String openRouterModel;

    private static final String OPENROUTER_URL = "https://openrouter.ai/api/v1/chat/completions";
    private static final String SITE_URL = "http://localhost:5173";
    private static final String SITE_NAME = "ChatApplication";

    private UUID botUserId;
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    public AiBotService(UserAccountRepository userAccountRepository,
                        ConversationAiService conversationAiService,
                        ChatMessageRepository chatMessageRepository,
                        ObjectMapper objectMapper) {
        this.userAccountRepository = userAccountRepository;
        this.conversationAiService = conversationAiService;
        this.chatMessageRepository = chatMessageRepository;
        this.objectMapper = objectMapper;
    }

    /**
     * Tạo tài khoản bot khi app khởi động nếu chưa tồn tại.
     */
    @PostConstruct
    @Transactional
    public void ensureBotUserExists() {
        Optional<UserAccount> existing = userAccountRepository.findByUsernameIgnoreCase(botUsername);
        if (existing.isPresent()) {
            this.botUserId = existing.get().getId();
            log.info("AI Bot user found: {} ({})", botUsername, botUserId);
            return;
        }

        UserAccount bot = new UserAccount();
        bot.setUsername(botUsername);
        bot.setDisplayName("AI Assistant 🤖");
        bot.setFullName("AI Chat Assistant");
        bot.setBio("Tôi là trợ lý AI thông minh. Hỏi tôi bất kỳ điều gì và tôi sẽ trả lời!");
        bot.setAvatarPath(null);
        UserAccount saved = userAccountRepository.save(bot);
        this.botUserId = saved.getId();
        log.info("AI Bot user created: {} ({})", botUsername, botUserId);
    }

    public UUID getBotUserId() {
        if (botUserId == null) {
            botUserId = userAccountRepository.findByUsernameIgnoreCase(botUsername)
                    .map(UserAccount::getId)
                    .orElse(null);
        }
        return botUserId;
    }

    public boolean isBotUser(UUID userId) {
        return userId != null && userId.equals(getBotUserId());
    }

    /**
     * Tạo phản hồi bằng OpenRouter AI. Fallback về rule-based nếu API key chưa set hoặc lỗi.
     */
    @Transactional(readOnly = true)
    public String generateBotReply(String userMessage, UUID conversationId) {
        if (userMessage == null || userMessage.isBlank()) {
            return helpMessage();
        }

        if (isVietnamTimeQuestion(userMessage)) {
            return buildVietnamTimeReply();
        }

        if (isOpenRouterCoolingDown(conversationId)) {
            log.debug("OpenRouter cooldown active for conversation {}. Using fallback reply.", conversationId);
            return ruleBasedReply(userMessage, conversationId);
        }

        // Nếu có API key → dùng OpenRouter
        if (openRouterApiKey != null && !openRouterApiKey.isBlank()) {
            try {
                return callOpenRouter(userMessage, conversationId);
            } catch (Exception e) {
                log.warn("OpenRouter call failed, falling back to rule-based: {}", e.getMessage());
            }
        }

        // Fallback: rule-based
        return ruleBasedReply(userMessage, conversationId);
    }

    private boolean isOpenRouterCoolingDown(UUID conversationId) {
        if (conversationId == null) {
            return false;
        }
        Instant until = openRouterCooldownUntil.get(conversationId);
        if (until == null) {
            return false;
        }
        if (Instant.now().isAfter(until)) {
            openRouterCooldownUntil.remove(conversationId, until);
            return false;
        }
        return true;
    }

    // ─────────────────── OpenRouter Integration ───────────────────────

    private String callOpenRouter(String userMessage, UUID conversationId) throws Exception {
        // Build context từ conversation insight + recent messages
        String systemPrompt = buildSystemPrompt(conversationId);
        List<String> recentMessages = loadRecentMessagesForContext(conversationId);

        // Build JSON body
        ObjectNode body = objectMapper.createObjectNode();
        body.put("model", openRouterModel);
        body.put("max_tokens", 600);
        body.put("temperature", 0.7);

        ArrayNode messages = body.putArray("messages");

        // System prompt
        ObjectNode systemMsg = messages.addObject();
        systemMsg.put("role", "system");
        systemMsg.put("content", systemPrompt);

        // Lịch sử tin nhắn gần đây làm context (tối đa 6 tin)
        if (!recentMessages.isEmpty()) {
            ObjectNode contextMsg = messages.addObject();
            contextMsg.put("role", "user");
            contextMsg.put("content", "Đây là các tin nhắn gần đây trong cuộc trò chuyện của user (không phải với bạn):\n" +
                    String.join("\n", recentMessages.stream().limit(6).collect(Collectors.toList())));

            ObjectNode contextAck = messages.addObject();
            contextAck.put("role", "assistant");
            contextAck.put("content", "Tôi đã đọc và hiểu ngữ cảnh cuộc trò chuyện. Sẵn sàng hỗ trợ!");
        }

        // User message hiện tại
        ObjectNode userMsg = messages.addObject();
        userMsg.put("role", "user");
        userMsg.put("content", userMessage);

        String bodyJson = objectMapper.writeValueAsString(body);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(OPENROUTER_URL))
                .timeout(Duration.ofSeconds(30))
                .header("Authorization", "Bearer " + openRouterApiKey)
                .header("Content-Type", "application/json")
                .header("HTTP-Referer", SITE_URL)
                .header("X-Title", SITE_NAME)
                .POST(HttpRequest.BodyPublishers.ofString(bodyJson))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 429) {
            openRouterCooldownUntil.put(conversationId, Instant.now().plus(OPENROUTER_COOLDOWN));
            log.warn("OpenRouter rate limited for conversation {}. Cooling down for {} minutes.", conversationId, OPENROUTER_COOLDOWN.toMinutes());
            return ruleBasedReply(userMessage, conversationId);
        }

        if (response.statusCode() != 200) {
            log.warn("OpenRouter returned status {}: {}", response.statusCode(), response.body());
            throw new RuntimeException("OpenRouter API error: " + response.statusCode());
        }

        JsonNode responseJson = objectMapper.readTree(response.body());
        String content = responseJson
                .path("choices")
                .path(0)
                .path("message")
                .path("content")
                .asText("");

        if (content.isBlank()) {
            throw new RuntimeException("Empty response from OpenRouter");
        }

        log.debug("OpenRouter reply for conversation {}: {} chars", conversationId, content.length());
        return content;
    }

    private String buildSystemPrompt(UUID conversationId) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("Bạn là AI Assistant 🤖 — một trợ lý thông minh tích hợp trong ứng dụng chat.\n");
        prompt.append("Bạn trả lời bằng tiếng Việt, ngắn gọn, thân thiện, hữu ích và chủ động.\n");
        prompt.append("Bạn là trợ lý đa năng: trả lời câu hỏi tự do, giải thích kiến thức, gợi ý nội dung, dịch thuật, tóm tắt, quản lý task, deadline, và hỗ trợ hội thoại thông thường.\n");
        prompt.append("Không được giới hạn câu trả lời chỉ vào tóm tắt hoặc task. Nếu user hỏi điều gì hợp lý thì hãy trả lời trực tiếp.\n");
        prompt.append("Nếu user hỏi giờ hiện tại ở Việt Nam, hãy dùng múi giờ Asia/Ho_Chi_Minh được cung cấp trong ngữ cảnh hệ thống.\n\n");

        ZonedDateTime vietnamNow = ZonedDateTime.now(VIETNAM_ZONE_ID);
        prompt.append("Thời gian hiện tại ở Việt Nam: ")
                .append(VIETNAM_TIME_FORMATTER.format(vietnamNow))
                .append(".\n\n");

        // Thêm insight context nếu có
        try {
            ConversationAiInsightResponse insight = conversationAiService.getConversationInsight(conversationId);
            if (insight != null && insight.getSummary() != null) {
                prompt.append("=== BỐI CẢNH CUỘC TRÒ CHUYỆN ===\n");
                prompt.append("Tóm tắt: ").append(insight.getSummary()).append("\n");
                if (insight.getFocusTopic() != null) {
                    prompt.append("Chủ đề chính: ").append(insight.getFocusTopic()).append("\n");
                }
                if (insight.getOpenTaskCount() > 0) {
                    prompt.append("Số task đang mở: ").append(insight.getOpenTaskCount()).append("\n");
                }

                // Thêm danh sách task nếu có
                if (insight.getTasks() != null && !insight.getTasks().isEmpty()) {
                    prompt.append("Tasks:\n");
                    DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy").withZone(ZoneId.systemDefault());
                    for (ConversationAiTaskResponse task : insight.getTasks()) {
                        prompt.append("- ").append(task.getTitle());
                        if (task.getDueAt() != null) {
                            prompt.append(" (deadline: ").append(fmt.format(task.getDueAt())).append(")");
                        }
                        prompt.append(" [").append(task.getPriority()).append("]");
                        if (task.isOverdue()) prompt.append(" ⚠️ QUÁ HẠN");
                        prompt.append("\n");
                    }
                }
                prompt.append("=================================\n\n");
            }
        } catch (Exception e) {
            log.debug("Could not load insight for system prompt: {}", e.getMessage());
        }

        prompt.append("Hãy trả lời câu hỏi/yêu cầu của user dựa trên ngữ cảnh trên nếu liên quan, nhưng vẫn phải xử lý tốt cả câu hỏi ngoài ngữ cảnh.");
        return prompt.toString();
    }

    private List<String> loadRecentMessagesForContext(UUID conversationId) {
        try {
            return chatMessageRepository.findByConversationIdOrderByCreatedAtDesc(conversationId,
                            org.springframework.data.domain.PageRequest.of(0, 10))
                    .getContent()
                    .stream()
                    .filter(m -> m.getContent() != null && !m.getContent().isBlank())
                    .filter(m -> !isBotUser(m.getSender().getId())) // Bỏ tin nhắn của bot
                    .map(m -> {
                        String sender = m.getSender().getDisplayName() != null
                                ? m.getSender().getDisplayName()
                                : m.getSender().getUsername();
                        return sender + ": " + m.getContent();
                    })
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.debug("Could not load recent messages: {}", e.getMessage());
            return List.of();
        }
    }

    // ─────────────────── Rule-based Fallback ──────────────────────────

    private String ruleBasedReply(String userMessage, UUID conversationId) {
        String normalized = userMessage.toLowerCase().trim();

        if (isVietnamTimeQuestion(normalized)) {
            return buildVietnamTimeReply();
        }

        if (matchesAny(normalized, "help", "giúp", "hướng dẫn", "lệnh", "commands")) {
            return helpMessage();
        }
        if (matchesAny(normalized, "tóm tắt", "tom tat", "summary", "tổng hợp", "tổng kết", "phân tích")) {
            return buildSummaryReply(conversationId);
        }
        if (matchesAny(normalized, "task", "việc cần", "công việc", "deadline", "cần làm", "phải làm", "giao việc", "làm xong")) {
            return buildTasksReply(conversationId);
        }
        if (matchesAny(normalized, "hello", "hi", "chào", "xin chào", "hey")) {
            return "👋 Xin chào! Tôi là AI Assistant.\n\n" +
                   "Bạn có thể hỏi bất kỳ điều gì: câu hỏi thường ngày, kiến thức chung, viết nội dung, tóm tắt, task, deadline, và nhiều hơn nữa.\n\n" +
                   "Nhắn **\"help\"** nếu muốn xem các gợi ý nhanh. 🚀";
        }
        return buildGeneralFallbackReply(userMessage);
    }

    private boolean matchesAny(String text, String... keywords) {
        for (String kw : keywords) {
            if (text.contains(kw)) return true;
        }
        return false;
    }

    private String buildSummaryReply(UUID conversationId) {
        try {
            ConversationAiInsightResponse insight = conversationAiService.getConversationInsight(conversationId);
            if (insight == null || insight.getSummary() == null) {
                return "📊 Chưa có đủ dữ liệu để tóm tắt. Hãy gửi thêm tin nhắn vào cuộc trò chuyện trước nhé!";
            }
            StringBuilder sb = new StringBuilder();
            sb.append("📊 **Tóm tắt cuộc trò chuyện:**\n\n");
            sb.append(insight.getSummary()).append("\n\n");
            if (insight.getSummaryBullets() != null && !insight.getSummaryBullets().isEmpty()) {
                for (String bullet : insight.getSummaryBullets()) {
                    sb.append("• ").append(bullet).append("\n");
                }
            }
            sb.append("\n_Từ ").append(insight.getSourceMessageCount()).append(" tin nhắn gần nhất._");
            return sb.toString();
        } catch (Exception e) {
            return "⚠️ Chưa thể truy cập dữ liệu. Hãy thử lại sau!";
        }
    }

    private String buildTasksReply(UUID conversationId) {
        try {
            List<ConversationAiTaskResponse> tasks = conversationAiService.listTasksForConversation(conversationId);
            if (tasks == null || tasks.isEmpty()) {
                return "✅ Chưa có task nào! Nhắn tin chứa từ \"cần\", \"deadline\", \"gửi\"... để AI nhận diện task.";
            }
            StringBuilder sb = new StringBuilder("📋 **Tasks (" + tasks.size() + "):**\n\n");
            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy").withZone(ZoneId.systemDefault());
            for (int i = 0; i < tasks.size(); i++) {
                ConversationAiTaskResponse task = tasks.get(i);
                String p = switch (task.getPriority() != null ? task.getPriority().toUpperCase() : "LOW") {
                    case "HIGH" -> "🔴";
                    case "MEDIUM" -> "🟡";
                    default -> "🟢";
                };
                sb.append(p).append(" ").append(i + 1).append(". ").append(task.getTitle());
                if (task.getDueAt() != null) {
                    sb.append(" — ").append(fmt.format(task.getDueAt()));
                    if (task.isOverdue()) sb.append(" ⚠️");
                }
                sb.append("\n");
            }
            return sb.toString();
        } catch (Exception e) {
            return "⚠️ Chưa thể truy cập danh sách task. Hãy thử lại!";
        }
    }

    private String helpMessage() {
        boolean hasApiKey = openRouterApiKey != null && !openRouterApiKey.isBlank();
        return "🤖 **AI Assistant" + (hasApiKey ? " (OpenRouter AI)" : "") + " — Lệnh:**\n\n" +
               "• **tóm tắt** — Tổng hợp nội dung cuộc trò chuyện\n" +
               "• **task** — Liệt kê các task + deadline được nhận diện\n" +
               "• **hỏi tự do** — Bạn có thể hỏi bất kỳ điều gì như kiến thức chung, viết nội dung, dịch, gợi ý, thời gian, v.v.\n" +
               "• **help** — Xem lại lệnh này\n\n" +
               (hasApiKey
                   ? "✨ Tôi đang dùng **" + openRouterModel + "** — hãy hỏi như đang trò chuyện với một trợ lý AI thực thụ.\n"
                   : "💡 Thêm `OPENROUTER_API_KEY` vào `.env` để tôi trả lời thông minh và linh hoạt hơn!\n");
    }

    private boolean isVietnamTimeQuestion(String text) {
        String normalized = text.toLowerCase().trim();
        return matchesAny(normalized,
                "mấy giờ", "may gio", "bao nhiêu giờ", "bao nhieu gio", "giờ việt nam", "gio viet nam",
                "giờ ở việt nam", "gio o viet nam", "current time", "what time", "time is it", "bây giờ là mấy giờ",
                "bay gio la may gio", "bây giờ mấy giờ", "bay gio may gio", "đang mấy giờ", "dang may gio");
    }

    private String buildVietnamTimeReply() {
        ZonedDateTime now = ZonedDateTime.now(VIETNAM_ZONE_ID);
        return "🕒 Bây giờ ở Việt Nam là **" + VIETNAM_TIME_FORMATTER.format(now) + "**.\n\n"
                + "Nếu bạn muốn, tôi cũng có thể chuyển sang múi giờ khác hoặc nhắc bạn theo thời gian cụ thể.";
    }

    private String buildGeneralFallbackReply(String userMessage) {
        return "💬 Tôi đã nhận câu hỏi của bạn.\n\n"
                + "Hiện tôi đang ở chế độ dự phòng nên chưa trả lời sâu như AI đầy đủ, nhưng bạn vẫn có thể hỏi bất kỳ chủ đề nào: kiến thức chung, lập trình, viết nội dung, dịch thuật, tóm tắt, task, deadline, hoặc câu hỏi thường ngày.\n\n"
                + "Hãy gửi lại câu hỏi theo cách cụ thể hơn, và nếu có OpenRouter tôi sẽ trả lời chi tiết hơn ngay.";
    }
}
