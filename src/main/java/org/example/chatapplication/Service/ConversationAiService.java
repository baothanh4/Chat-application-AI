package org.example.chatapplication.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.chatapplication.DTO.Response.ConversationAiInsightResponse;
import org.example.chatapplication.DTO.Response.ConversationAiTaskResponse;
import org.example.chatapplication.Model.Entity.ChatMessage;
import org.example.chatapplication.Model.Entity.Conversation;
import org.example.chatapplication.Model.Entity.ConversationAiInsight;
import org.example.chatapplication.Model.Entity.ConversationTaskInsight;
import org.example.chatapplication.Model.Entity.UserAccount;
import org.example.chatapplication.Repository.ChatMessageRepository;
import org.example.chatapplication.Repository.ConversationAiInsightRepository;
import org.example.chatapplication.Repository.ConversationTaskInsightRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

@Service
@RequiredArgsConstructor
@Slf4j
public class ConversationAiService {
    private static final int MAX_MESSAGES = 40;
    private static final int MAX_TASKS = 8;
    private static final ZoneId ZONE_ID = ZoneId.systemDefault();
    private static final DateTimeFormatter OUTPUT_DATE = DateTimeFormatter.ofPattern("dd/MM HH:mm");
    private static final Pattern DATE_PATTERN = Pattern.compile("(?:ngay\\s*)?(\\d{1,2})[./-](\\d{1,2})(?:[./-](\\d{2,4}))?", Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
    private static final Pattern DAYS_PATTERN = Pattern.compile("trong\\s+(\\d{1,2})\\s+ngay", Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
    private static final List<String> TASK_KEYWORDS = List.of(
            "cần", "phải", "deadline", "gửi", "review", "hoàn thành", "triển khai", "sửa", "fix", "test", "check", "làm", "update", "deploy", "submit"
    );
    private static final Set<String> STOP_WORDS = Set.of(
            "và", "của", "cho", "một", "các", "trong", "này", "đó", "sẽ", "được", "với", "khi", "thì", "là", "để", "the", "a", "an", "to", "of", "in", "on"
    );

    private final ConversationService conversationService;
    private final ChatMessageRepository chatMessageRepository;
    private final ConversationAiInsightRepository insightRepository;
    private final ConversationTaskInsightRepository taskRepository;
    private final OfflineLlmService offlineLlmService;
    private final ObjectMapper objectMapper;
    private final Clock clock = Clock.systemDefaultZone();

    @Transactional(readOnly = true)
    public ConversationAiInsightResponse getConversationInsight(UUID conversationId) {
        Conversation conversation = conversationService.requireConversation(conversationId);
        return insightRepository.findByConversationId(conversationId)
                .map(this::toResponse)
                .orElseGet(() -> emptyResponse(conversation));
    }

    @Transactional
    public ConversationAiInsightResponse refreshConversationInsight(UUID conversationId) {
        Conversation conversation = conversationService.requireConversation(conversationId);
        List<ChatMessage> messages = loadRecentMessages(conversationId);
        
        boolean useLlm = messages.size() >= 5; // Only use LLM if there's enough context
        AnalysisResult analysis;
        String modelName = "heuristic-ai-v1";

        if (useLlm) {
            AnalysisResult llmResult = analyzeConversationWithLlm(conversation, messages);
            if (llmResult != null) {
                analysis = llmResult;
                modelName = "ollama-" + offlineLlmService.getOllamaModel();
            } else {
                analysis = analyzeConversation(conversation, messages);
            }
        } else {
            analysis = analyzeConversation(conversation, messages);
        }

        ConversationAiInsight insight = insightRepository.findByConversationId(conversationId)
                .orElseGet(ConversationAiInsight::new);
        insight.setConversation(conversation);
        insight.setSummary(analysis.summary());
        insight.setSummaryBullets(String.join("\n", analysis.bullets()));
        insight.setFocusTopic(analysis.focusTopic());
        insight.setModelName(modelName);
        insight.setGeneratedAt(Instant.now(clock));
        insight.setSourceMessageCount(messages.size());
        insight.setSourceLatestMessageAt(messages.isEmpty() ? null : messages.getLast().getCreatedAt());
        insight.getTasks().clear();

        int sortOrder = 0;
        for (DetectedTask task : analysis.tasks()) {
            ConversationTaskInsight taskEntity = new ConversationTaskInsight();
            taskEntity.setInsight(insight);
            taskEntity.setTitle(task.title());
            taskEntity.setDescription(task.description());
            taskEntity.setSourceMessageSnippet(task.sourceMessageSnippet());
            taskEntity.setDueAt(task.dueAt());
            taskEntity.setPriority(task.priority());
            taskEntity.setStatus(task.status());
            taskEntity.setConfidenceScore(task.confidenceScore());
            taskEntity.setSortOrder(sortOrder++);
            insight.getTasks().add(taskEntity);
        }

        ConversationAiInsight saved = insightRepository.save(insight);
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<ConversationAiTaskResponse> listTasksForUser(UUID userId) {
        return taskRepository.findAllForUser(userId).stream()
                .map(task -> toTaskResponse(task, task.getInsight().getConversation()))
                .sorted(this::compareTaskResponses)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ConversationAiTaskResponse> listTasksForConversation(UUID conversationId) {
        Conversation conversation = conversationService.requireConversation(conversationId);
        return insightRepository.findByConversationId(conversationId)
                .map(insight -> insight.getTasks().stream()
                        .map(task -> toTaskResponse(task, conversation))
                        .sorted(this::compareTaskResponses)
                        .toList())
                .orElseGet(List::of);
    }

    private List<ChatMessage> loadRecentMessages(UUID conversationId) {
        Page<ChatMessage> page = chatMessageRepository.findByConversationIdOrderByCreatedAtDesc(
                conversationId,
                PageRequest.of(0, MAX_MESSAGES, Sort.by(Sort.Direction.DESC, "createdAt"))
        );
        List<ChatMessage> messages = new ArrayList<>(page.getContent());
        messages.sort(Comparator.comparing(ChatMessage::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder())));
        return messages;
    }

    private AnalysisResult analyzeConversation(Conversation conversation, List<ChatMessage> messages) {
        if (messages.isEmpty()) {
            return new AnalysisResult(
                    "Chưa có tin nhắn đủ để AI tổng hợp.",
                    List.of("Hãy bắt đầu gửi tin nhắn để hệ thống tự động tóm tắt và tạo task."),
                    conversation.getName(),
                    List.of()
            );
        }

        Map<String, Long> senderCounts = new LinkedHashMap<>();
        for (ChatMessage message : messages) {
            String senderName = Optional.ofNullable(message.getSender())
                    .map(UserAccount::getDisplayName)
                    .filter(name -> !name.isBlank())
                    .orElse(Optional.ofNullable(message.getSender()).map(UserAccount::getUsername).orElse("Ẩn danh"));
            senderCounts.merge(senderName, 1L, Long::sum);
        }

        List<String> topSenders = senderCounts.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(3)
                .map(entry -> entry.getKey() + " (" + entry.getValue() + ")")
                .toList();

        Map<String, Long> topicScores = extractTopicScores(messages);
        List<String> topTopics = topicScores.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(4)
                .map(Map.Entry::getKey)
                .toList();

        List<DetectedTask> tasks = detectTasks(messages);
        Instant nextDeadline = tasks.stream()
                .map(DetectedTask::dueAt)
                .filter(Objects::nonNull)
                .min(Comparator.naturalOrder())
                .orElse(null);

        List<String> bullets = new ArrayList<>();
        bullets.add("Cuộc trò chuyện có " + senderCounts.size() + " người tham gia, nổi bật: " + String.join(", ", topSenders));
        if (!topTopics.isEmpty()) {
            bullets.add("Chủ đề nổi bật: " + String.join(", ", topTopics));
        }
        bullets.add("AI đã nhận diện " + tasks.size() + " task có thể theo dõi.");
        if (nextDeadline != null) {
            bullets.add("Deadline gần nhất: " + formatDueAt(nextDeadline));
        }

        String summary = "Cuộc trò chuyện đang tập trung vào "
                + (topTopics.isEmpty() ? "các cập nhật chung" : String.join(", ", topTopics))
                + ". "
                + "Những người tham gia chính: " + String.join(", ", topSenders) + ".";

        return new AnalysisResult(summary, bullets, topTopics.isEmpty() ? conversation.getName() : topTopics.getFirst(), tasks);
    }

    private AnalysisResult analyzeConversationWithLlm(Conversation conversation, List<ChatMessage> messages) {
        if (messages.isEmpty()) {
            return null;
        }

        StringBuilder chatContext = new StringBuilder();
        for (ChatMessage m : messages) {
            String sender = m.getSender() != null ? m.getSender().getDisplayName() : "Unknown";
            chatContext.append(sender).append(": ").append(m.getContent()).append("\n");
        }

        String prompt = """
                Hãy phân tích đoạn chat dưới đây và trả về kết quả dưới định dạng JSON duy nhất. 
                Nội dung JSON phải bao gồm:
                - "summary": Tóm tắt ngắn gọn cuộc hội thoại (tiếng Việt).
                - "bullets": Các điểm chính quan trọng (mảng chuỗi, tiếng Việt).
                - "focusTopic": Chủ đề cốt lõi của cuộc trò chuyện.
                - "tasks": Mảng các công việc hoặc deadline được nhắc tới. Mỗi phần tử gồm:
                   - "title": Tên task (tiếng Việt).
                   - "description": Chi tiết công việc.
                   - "dueAt": Thời hạn (định dạng ISO 8601, hoặc null nếu không rõ).
                   - "priority": Độ ưu tiên (HIGH, MEDIUM, LOW).

                Đoạn chat:
                %s

                Lưu ý: Chỉ trả về JSON nguyên bản, không bao gồm lời dẫn hay giải thích.
                """.formatted(chatContext.toString());

        log.info("Requesting LLM analysis for conversation {}", conversation.getId());
        Optional<String> response = offlineLlmService.generateWithOllama(prompt, 0.2, 1024);

        if (response.isEmpty()) {
            log.warn("LLM analysis returned empty response for conversation {}", conversation.getId());
            return null;
        }

        try {
            String jsonStr = response.get().trim();
            // Clean up markdown if present
            if (jsonStr.contains("```json")) {
                jsonStr = jsonStr.substring(jsonStr.indexOf("```json") + 7, jsonStr.lastIndexOf("```")).trim();
            } else if (jsonStr.contains("```")) {
                jsonStr = jsonStr.substring(jsonStr.indexOf("```") + 3, jsonStr.lastIndexOf("```")).trim();
            }

            JsonNode root = objectMapper.readTree(jsonStr);
            String summary = root.path("summary").asText("Không có tóm tắt.");
            List<String> bullets = new ArrayList<>();
            root.path("bullets").forEach(node -> bullets.add(node.asText()));
            String focusTopic = root.path("focusTopic").asText(conversation.getName());

            List<DetectedTask> tasks = new ArrayList<>();
            root.path("tasks").forEach(t -> {
                Instant dueAt = null;
                String dueStr = t.path("dueAt").asText(null);
                if (dueStr != null && !dueStr.isBlank() && !dueStr.equalsIgnoreCase("null")) {
                    try {
                        dueAt = Instant.parse(dueStr);
                    } catch (Exception ignored) {
                        // Attempt some basic fallback parsing if ISO fails
                    }
                }

                tasks.add(new DetectedTask(
                        t.path("title").asText("Công việc mới"),
                        t.path("description").asText(""),
                        "", // Snippet is hard to isolate from LLM response
                        dueAt,
                        t.path("priority").asText("MEDIUM").toUpperCase(),
                        "OPEN",
                        0.9
                ));
            });

            log.info("LLM analysis successful for conversation {}", conversation.getId());
            return new AnalysisResult(summary, bullets, focusTopic, tasks);
        } catch (Exception e) {
            log.error("Failed to parse LLM analysis JSON: {}", e.getMessage());
            return null;
        }
    }

    private Map<String, Long> extractTopicScores(List<ChatMessage> messages) {
        Map<String, Long> scores = new HashMap<>();
        for (ChatMessage message : messages) {
            for (String token : tokenize(message.getContent())) {
                if (token.length() < 4 || STOP_WORDS.contains(token)) {
                    continue;
                }
                scores.merge(token, 1L, Long::sum);
            }
        }
        return scores;
    }

    private List<String> tokenize(String content) {
        if (content == null || content.isBlank()) {
            return List.of();
        }
        return List.of(content.toLowerCase(Locale.ROOT)
                .replaceAll("[^\\p{L}\\p{Nd}\\s]", " ")
                .split("\\s+"));
    }

    private List<DetectedTask> detectTasks(List<ChatMessage> messages) {
        Map<String, DetectedTask> tasks = new LinkedHashMap<>();
        for (ChatMessage message : messages) {
            String content = Optional.ofNullable(message.getContent()).orElse("").trim();
            if (content.isBlank()) {
                continue;
            }

            for (String sentence : splitSentences(content)) {
                String normalized = sentence.toLowerCase(Locale.ROOT);
                if (TASK_KEYWORDS.stream().noneMatch(normalized::contains)) {
                    continue;
                }

                Instant dueAt = detectDueAt(normalized);
                String title = buildTaskTitle(sentence);
                String key = title.toLowerCase(Locale.ROOT);
                if (tasks.containsKey(key)) {
                    continue;
                }

                String priority = derivePriority(normalized, dueAt);
                double confidence = deriveConfidence(normalized, dueAt);
                tasks.put(key, new DetectedTask(
                        title,
                        buildTaskDescription(sentence, content),
                        excerpt(content),
                        dueAt,
                        priority,
                        "OPEN",
                        confidence
                ));
            }
        }

        return tasks.values().stream()
                .sorted(Comparator.comparing(DetectedTask::dueAt, Comparator.nullsLast(Comparator.naturalOrder())))
                .limit(MAX_TASKS)
                .toList();
    }

    private List<String> splitSentences(String content) {
        String[] raw = content.split("(?<=[.!?])\\s+|\\n+");
        List<String> sentences = new ArrayList<>();
        for (String piece : raw) {
            String cleaned = piece == null ? "" : piece.trim();
            if (!cleaned.isBlank()) {
                sentences.add(cleaned);
            }
        }
        return sentences;
    }

    private Instant detectDueAt(String normalizedSentence) {
        Instant now = Instant.now(clock);
        if (normalizedSentence.contains("hôm nay") || normalizedSentence.contains("hom nay")) {
            return ZonedDateTime.ofInstant(now, ZONE_ID).withHour(18).withMinute(0).withSecond(0).withNano(0).toInstant();
        }
        if (normalizedSentence.contains("mai")) {
            return ZonedDateTime.ofInstant(now, ZONE_ID).plusDays(1).withHour(18).withMinute(0).withSecond(0).withNano(0).toInstant();
        }

        Matcher daysMatcher = DAYS_PATTERN.matcher(normalizedSentence);
        if (daysMatcher.find()) {
            int days = Integer.parseInt(daysMatcher.group(1));
            return ZonedDateTime.ofInstant(now, ZONE_ID).plusDays(days).withHour(18).withMinute(0).withSecond(0).withNano(0).toInstant();
        }

        Matcher dateMatcher = DATE_PATTERN.matcher(normalizedSentence);
        if (dateMatcher.find()) {
            int day = Integer.parseInt(dateMatcher.group(1));
            int month = Integer.parseInt(dateMatcher.group(2));
            int year = dateMatcher.group(3) == null ? LocalDate.now(clock).getYear() : parseYear(dateMatcher.group(3));
            try {
                return LocalDate.of(year, month, day).atTime(18, 0).atZone(ZONE_ID).toInstant();
            } catch (DateTimeParseException | IllegalArgumentException ignored) {
                return null;
            }
        }

        if (normalizedSentence.contains("cuối tuần") || normalizedSentence.contains("cuoi tuan")) {
            LocalDate nowDate = LocalDate.now(clock);
            int daysUntilSaturday = Math.floorMod(6 - nowDate.getDayOfWeek().getValue(), 7);
            if (daysUntilSaturday == 0) {
                daysUntilSaturday = 7;
            }
            return nowDate.plusDays(daysUntilSaturday).atTime(17, 0).atZone(ZONE_ID).toInstant();
        }

        return null;
    }

    private int parseYear(String yearText) {
        int year = Integer.parseInt(yearText);
        return year < 100 ? 2000 + year : year;
    }

    private String derivePriority(String normalizedSentence, Instant dueAt) {
        if (normalizedSentence.contains("gấp") || normalizedSentence.contains("urgent") || normalizedSentence.contains("ngay")) {
            return "HIGH";
        }
        if (dueAt == null) {
            return "MEDIUM";
        }
        long days = Duration.between(Instant.now(clock), dueAt).toDays();
        if (days <= 1) {
            return "HIGH";
        }
        if (days <= 3) {
            return "MEDIUM";
        }
        return "LOW";
    }

    private double deriveConfidence(String normalizedSentence, Instant dueAt) {
        double score = 0.58;
        if (dueAt != null) {
            score += 0.2;
        }
        if (normalizedSentence.contains("phải") || normalizedSentence.contains("cần") || normalizedSentence.contains("gửi") || normalizedSentence.contains("review")) {
            score += 0.12;
        }
        if (normalizedSentence.length() > 60) {
            score += 0.05;
        }
        return Math.min(score, 0.98);
    }

    private String buildTaskTitle(String sentence) {
        String cleaned = sentence.replaceAll("\\s+", " ").trim();
        if (cleaned.length() <= 80) {
            return capitalizeFirst(cleaned);
        }
        return capitalizeFirst(cleaned.substring(0, 77).trim()) + "...";
    }

    private String buildTaskDescription(String sentence, String originalMessage) {
        if (sentence.equals(originalMessage)) {
            return sentence;
        }
        return sentence + " — từ: " + excerpt(originalMessage);
    }

    private String excerpt(String text) {
        String normalized = text.replaceAll("\\s+", " ").trim();
        if (normalized.length() <= 180) {
            return normalized;
        }
        return normalized.substring(0, 177).trim() + "...";
    }

    private String capitalizeFirst(String text) {
        if (text == null || text.isBlank()) {
            return "Task";
        }
        return Character.toUpperCase(text.charAt(0)) + text.substring(1);
    }

    private ConversationAiInsightResponse emptyResponse(Conversation conversation) {
        return new ConversationAiInsightResponse(
                conversation.getId(),
                conversation.getName(),
                "Chưa có tin nhắn đủ để AI tổng hợp.",
                List.of("Hãy gửi vài tin nhắn đầu tiên để hệ thống tự động tạo summary và task."),
                conversation.getDescription(),
                null,
                null,
                0,
                null,
                0,
                0,
                List.of()
        );
    }

    private ConversationAiInsightResponse toResponse(ConversationAiInsight insight) {
        List<String> bullets = insight.getSummaryBullets() == null || insight.getSummaryBullets().isBlank()
                ? List.of()
                : List.of(insight.getSummaryBullets().split("\\R"));
        List<ConversationAiTaskResponse> tasks = insight.getTasks().stream()
                .map(task -> toTaskResponse(task, insight.getConversation()))
                .sorted(this::compareTaskResponses)
                .toList();
        Instant nextDeadline = tasks.stream()
                .map(ConversationAiTaskResponse::getDueAt)
                .filter(Objects::nonNull)
                .min(Comparator.naturalOrder())
                .orElse(null);
        long overdueCount = tasks.stream().filter(ConversationAiTaskResponse::isOverdue).count();
        long openTaskCount = tasks.stream().filter(task -> "OPEN".equalsIgnoreCase(task.getStatus())).count();

        return new ConversationAiInsightResponse(
                insight.getConversation().getId(),
                insight.getConversation().getName(),
                insight.getSummary(),
                bullets,
                insight.getFocusTopic(),
                insight.getGeneratedAt(),
                insight.getSourceLatestMessageAt(),
                insight.getSourceMessageCount(),
                nextDeadline,
                openTaskCount,
                overdueCount,
                tasks
        );
    }

    private ConversationAiTaskResponse toTaskResponse(ConversationTaskInsight task, Conversation conversation) {
        boolean overdue = task.getDueAt() != null && task.getDueAt().isBefore(Instant.now(clock)) && "OPEN".equalsIgnoreCase(task.getStatus());
        long daysRemaining = task.getDueAt() == null ? -1 : Duration.between(Instant.now(clock), task.getDueAt()).toDays();
        return new ConversationAiTaskResponse(
                task.getId(),
                conversation.getId(),
                conversation.getName(),
                task.getTitle(),
                task.getDescription(),
                task.getSourceMessageSnippet(),
                task.getDueAt(),
                task.getPriority(),
                task.getStatus(),
                task.getConfidenceScore(),
                daysRemaining,
                overdue
        );
    }

    private int compareTaskResponses(ConversationAiTaskResponse left, ConversationAiTaskResponse right) {
        int leftRank = priorityRank(left.getPriority());
        int rightRank = priorityRank(right.getPriority());
        if (leftRank != rightRank) {
            return Integer.compare(leftRank, rightRank);
        }
        if (left.getDueAt() == null && right.getDueAt() == null) {
            return 0;
        }
        if (left.getDueAt() == null) {
            return 1;
        }
        if (right.getDueAt() == null) {
            return -1;
        }
        return left.getDueAt().compareTo(right.getDueAt());
    }

    private int priorityRank(String priority) {
        if (priority == null) {
            return 2;
        }
        return switch (priority.toUpperCase(Locale.ROOT)) {
            case "HIGH" -> 0;
            case "MEDIUM" -> 1;
            default -> 2;
        };
    }

    private String formatDueAt(Instant dueAt) {
        return OUTPUT_DATE.format(dueAt.atZone(ZONE_ID));
    }

    private record AnalysisResult(String summary, List<String> bullets, String focusTopic, List<DetectedTask> tasks) { }

    private record DetectedTask(String title, String description, String sourceMessageSnippet, Instant dueAt, String priority, String status, double confidenceScore) { }
}

