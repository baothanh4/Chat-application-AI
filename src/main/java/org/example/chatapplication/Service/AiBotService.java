package org.example.chatapplication.Service;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.example.chatapplication.DTO.Response.ConversationAiInsightResponse;
import org.example.chatapplication.DTO.Response.ConversationAiTaskResponse;
import org.example.chatapplication.Model.Entity.Conversation;
import org.example.chatapplication.Model.Entity.UserAccount;
import org.example.chatapplication.Repository.ChatMessageRepository;
import org.example.chatapplication.Repository.UserAccountRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Slf4j
public class AiBotService {

    private static final ZoneId VIETNAM_ZONE_ID = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final DateTimeFormatter VIETNAM_TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm, dd/MM/yyyy");

    private final UserAccountRepository userAccountRepository;
    private final ConversationAiService conversationAiService;
    private final ConversationService conversationService;
    private final ChatMessageRepository chatMessageRepository;
    private final OfflineLlmService offlineLlmService;

    @Value("${chatbot.bot-username:ai-assistant}")
    private String botUsername;

    @Value("${chatbot.local.enabled:true}")
    private boolean localAiEnabled;

    @Value("${chatbot.local.default-temperature:0.7}")
    private double defaultTemperature;

    @Value("${chatbot.local.default-max-tokens:512}")
    private int defaultMaxTokens;

    private UUID botUserId;

    public AiBotService(UserAccountRepository userAccountRepository,
                        ConversationAiService conversationAiService,
                        ConversationService conversationService,
                        ChatMessageRepository chatMessageRepository,
                        OfflineLlmService offlineLlmService) {
        this.userAccountRepository = userAccountRepository;
        this.conversationAiService = conversationAiService;
        this.conversationService = conversationService;
        this.chatMessageRepository = chatMessageRepository;
        this.offlineLlmService = offlineLlmService;
    }

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

    @Transactional(readOnly = true)
    public String generateBotReply(String userMessage, UUID conversationId) {
        if (userMessage == null || userMessage.isBlank()) {
            return helpMessage();
        }

        if (isVietnamTimeQuestion(userMessage)) {
            return buildVietnamTimeReply();
        }

        String normalized = userMessage.toLowerCase().trim();
        if (matchesAny(normalized, "help", "giup", "giúp", "huong dan", "hướng dẫn", "lenh", "lệnh", "commands")) {
            return helpMessage();
        }
        if (matchesAny(normalized, "tom tat", "tóm tắt", "summary", "tong hop", "tổng hợp", "tong ket", "tổng kết", "phan tich", "phân tích")) {
            return buildSummaryReply(conversationId);
        }
        if (matchesAny(normalized, "task", "viec can", "việc cần", "cong viec", "công việc", "deadline", "can lam", "cần làm", "phai lam", "phải làm", "giao viec", "giao việc", "lam xong", "làm xong")) {
            return buildTasksReply(conversationId);
        }

        Conversation conversation = null;
        try {
            conversation = conversationService.requireConversation(conversationId);
        } catch (Exception ex) {
            log.debug("Conversation {} unavailable for AI context: {}", conversationId, ex.getMessage());
        }

        if (!localAiEnabled || (conversation != null && Boolean.FALSE.equals(conversation.getAiUseOfflineModel()))) {
            return ruleBasedReply(userMessage, conversationId);
        }

        String prompt = buildModelPrompt(userMessage, conversationId, conversation);
        Double temperature = conversation != null && conversation.getAiTemperature() != null
                ? conversation.getAiTemperature()
                : defaultTemperature;
        Integer maxTokens = conversation != null && conversation.getAiMaxTokens() != null
                ? conversation.getAiMaxTokens()
                : defaultMaxTokens;

        Optional<String> modelReply = offlineLlmService.generateWithOllama(prompt, temperature, maxTokens);
        if (modelReply.isPresent()) {
            return modelReply.get();
        }

        return ruleBasedReply(userMessage, conversationId);
    }

    private String buildModelPrompt(String userMessage, UUID conversationId, Conversation conversation) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("Ban la AI assistant trong ung dung chat noi bo.\n");
        prompt.append("Tra loi bang tieng Viet, tu nhien, ngan gon, than thien va huu ich.\n");
        prompt.append("Neu cau hoi can liet ke, su dung bullet ro rang.\n");

        if (conversation != null && conversation.getAiSystemPrompt() != null) {
            prompt.append("\n[Cau hinh system theo phong]\n")
                    .append(conversation.getAiSystemPrompt())
                    .append("\n");
        }

        if (conversation != null && conversation.getAiBehaviorPrompt() != null) {
            prompt.append("\n[Phong cach tra loi theo phong]\n")
                    .append(conversation.getAiBehaviorPrompt())
                    .append("\n");
        }

        ZonedDateTime vietnamNow = ZonedDateTime.now(VIETNAM_ZONE_ID);
        prompt.append("\n[Thoi gian Viet Nam hien tai] ")
                .append(VIETNAM_TIME_FORMATTER.format(vietnamNow))
                .append("\n");

        try {
            ConversationAiInsightResponse insight = conversationAiService.getConversationInsight(conversationId);
            if (insight != null && insight.getSummary() != null) {
                prompt.append("\n[Tong quan cuoc tro chuyen]\n");
                prompt.append("Tom tat: ").append(insight.getSummary()).append("\n");
                if (insight.getFocusTopic() != null) {
                    prompt.append("Chu de chinh: ").append(insight.getFocusTopic()).append("\n");
                }
                if (insight.getOpenTaskCount() > 0) {
                    prompt.append("So task dang mo: ").append(insight.getOpenTaskCount()).append("\n");
                }
            }
        } catch (Exception ex) {
            log.debug("Could not attach insight to model prompt: {}", ex.getMessage());
        }

        List<String> recentMessages = loadRecentMessagesForContext(conversationId);
        if (!recentMessages.isEmpty()) {
            prompt.append("\n[Ngu canh tin nhan gan day]\n");
            recentMessages.stream().limit(8).forEach(m -> prompt.append("- ").append(m).append("\n"));
        }

        prompt.append("\n[Cau hoi hien tai cua user]\n")
                .append(userMessage.strip())
                .append("\n\nTra loi truc tiep vao cau hoi tren.");

        return prompt.toString();
    }

    private List<String> loadRecentMessagesForContext(UUID conversationId) {
        try {
            return chatMessageRepository.findByConversationIdOrderByCreatedAtDesc(conversationId, PageRequest.of(0, 12))
                    .getContent()
                    .stream()
                    .filter(m -> m.getContent() != null && !m.getContent().isBlank())
                    .filter(m -> !isBotUser(m.getSender().getId()))
                    .map(m -> {
                        String sender = m.getSender().getDisplayName() != null
                                ? m.getSender().getDisplayName()
                                : m.getSender().getUsername();
                        return sender + ": " + m.getContent();
                    })
                    .collect(Collectors.toList());
        } catch (Exception ex) {
            log.debug("Could not load recent messages for AI prompt: {}", ex.getMessage());
            return List.of();
        }
    }

    private String ruleBasedReply(String userMessage, UUID conversationId) {
        String normalized = userMessage.toLowerCase().trim();

        if (isVietnamTimeQuestion(normalized)) {
            return buildVietnamTimeReply();
        }
        if (matchesAny(normalized, "help", "giup", "giúp", "huong dan", "hướng dẫn", "lenh", "lệnh", "commands")) {
            return helpMessage();
        }
        if (matchesAny(normalized, "tom tat", "tóm tắt", "summary", "tong hop", "tổng hợp", "tong ket", "tổng kết", "phan tich", "phân tích")) {
            return buildSummaryReply(conversationId);
        }
        if (matchesAny(normalized, "task", "viec can", "việc cần", "cong viec", "công việc", "deadline", "can lam", "cần làm", "phai lam", "phải làm", "giao viec", "giao việc", "lam xong", "làm xong")) {
            return buildTasksReply(conversationId);
        }
        if (matchesAny(normalized, "hello", "hi", "chao", "chào", "xin chao", "xin chào", "hey")) {
            return "Xin chao, toi la AI Assistant. Ban co the hoi kien thuc chung, lap trinh, tom tat, task, deadline hoac nhan tu van noi dung.";
        }
        return buildGeneralFallbackReply();
    }

    private String buildSummaryReply(UUID conversationId) {
        try {
            ConversationAiInsightResponse insight = conversationAiService.getConversationInsight(conversationId);
            if (insight == null || insight.getSummary() == null) {
                return "Chua co du du lieu de tom tat. Hay gui them tin nhan vao cuoc tro chuyen.";
            }

            StringBuilder sb = new StringBuilder();
            sb.append("Tom tat cuoc tro chuyen:\n\n");
            sb.append(insight.getSummary()).append("\n");

            if (insight.getSummaryBullets() != null && !insight.getSummaryBullets().isEmpty()) {
                for (String bullet : insight.getSummaryBullets()) {
                    sb.append("- ").append(bullet).append("\n");
                }
            }
            return sb.toString();
        } catch (Exception ex) {
            return "Khong the tao tom tat luc nay. Hay thu lai sau.";
        }
    }

    private String buildTasksReply(UUID conversationId) {
        try {
            List<ConversationAiTaskResponse> tasks = conversationAiService.listTasksForConversation(conversationId);
            if (tasks == null || tasks.isEmpty()) {
                return "Chua co task nao duoc nhan dien trong cuoc tro chuyen.";
            }

            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy").withZone(ZoneId.systemDefault());
            StringBuilder sb = new StringBuilder("Danh sach task:\n");
            for (int i = 0; i < tasks.size(); i++) {
                ConversationAiTaskResponse task = tasks.get(i);
                sb.append(i + 1).append(". ").append(task.getTitle());
                if (task.getDueAt() != null) {
                    sb.append(" (deadline: ").append(fmt.format(task.getDueAt())).append(")");
                }
                if (task.isOverdue()) {
                    sb.append(" [QUA HAN]");
                }
                sb.append("\n");
            }
            return sb.toString();
        } catch (Exception ex) {
            return "Khong the doc task luc nay. Hay thu lai sau.";
        }
    }

    private String helpMessage() {
        return "AI Assistant (Offline Model) - Lenh nhanh:\n"
                + "- tom tat: tong hop noi dung cuoc tro chuyen\n"
                + "- task: liet ke task + deadline\n"
                + "- hoi tu do: hoi kien thuc chung, lap trinh, viet noi dung, dich thuat\n"
                + "- help: hien tro giup\n\n"
                + "Model offline hien tai: " + offlineLlmService.getOllamaModel();
    }

    private boolean isVietnamTimeQuestion(String text) {
        String normalized = text.toLowerCase().trim();
        return matchesAny(normalized,
                "mấy giờ", "may gio", "bao nhieu gio", "bao nhiêu giờ", "gio viet nam", "giờ việt nam",
                "current time", "what time", "time is it", "bay gio la may gio", "bây giờ là mấy giờ");
    }

    private String buildVietnamTimeReply() {
        ZonedDateTime now = ZonedDateTime.now(VIETNAM_ZONE_ID);
        return "Bay gio o Viet Nam la " + VIETNAM_TIME_FORMATTER.format(now) + ".";
    }

    private String buildGeneralFallbackReply() {
        return "Toi da nhan cau hoi cua ban. Hien model offline tam thoi khong phan hoi duoc, vui long thu lai hoac dat cau hoi cu the hon.";
    }

    private boolean matchesAny(String text, String... keywords) {
        for (String keyword : keywords) {
            if (text.contains(keyword)) {
                return true;
            }
        }
        return false;
    }
}
