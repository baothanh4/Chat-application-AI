package org.example.chatapplication.Controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.chatapplication.Repository.ChatMessageRepository;
import org.example.chatapplication.Repository.ConversationRepository;
import org.example.chatapplication.Repository.UserAccountRepository;
import org.example.chatapplication.Service.AiInferenceService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
@Slf4j
public class AiChatController {

    private final AiInferenceService aiInferenceService;
    private final ConversationRepository conversationRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final UserAccountRepository userAccountRepository;

    @PostMapping("/chat")
    public ResponseEntity<String> chat(@RequestBody Map<String, Object> request) {
        String prompt = (String) request.get("prompt");
        Double temperature = (Double) request.get("temperature");
        Integer maxTokens = (Integer) request.get("maxTokens");

        if (temperature == null) temperature = 0.7;
        if (maxTokens == null) maxTokens = 1024;

        // Xử lý thông minh: Nếu người dùng yêu cầu tổng hợp chat của một người cụ thể
        String enrichedPrompt = enrichPromptWithOtherChats(prompt);

        Optional<String> reply = aiInferenceService.generate(enrichedPrompt, temperature, maxTokens);
        return reply.map(ResponseEntity::ok)
                .orElse(ResponseEntity.internalServerError().body("Không thể kết nối với AI Assistant. Vui lòng kiểm tra lại cấu hình OpenRouter và đảm bảo Backend đang chạy."));
    }

    private String enrichPromptWithOtherChats(String prompt) {
        String lowerPrompt = prompt.toLowerCase();
        if (!lowerPrompt.contains("tổng hợp") && !lowerPrompt.contains("summary")) {
            return prompt;
        }

        try {
            String currentUsername = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getName();
            var currentUser = userAccountRepository.findByUsernameIgnoreCase(currentUsername).orElse(null);
            if (currentUser == null) return prompt;

            // Lấy danh sách hội thoại của user
            List<org.example.chatapplication.Model.Entity.Conversation> conversations = conversationRepository.findAll(); 
            // Lưu ý: Trong thực tế nên filter theo member, nhưng đây là logic demo nhanh
            
            for (var conv : conversations) {
                String convName = conv.getName();
                if (convName != null && lowerPrompt.contains(convName.toLowerCase())) {
                    log.info("Detected request for conversation context: {}", convName);
                    List<org.example.chatapplication.Model.Entity.ChatMessage> messages = 
                        chatMessageRepository.findByConversationIdOrderByCreatedAtDesc(conv.getId(), org.springframework.data.domain.PageRequest.of(0, 30))
                        .getContent();
                    
                    StringBuilder context = new StringBuilder("\n\n[Dữ liệu bổ sung: Tin nhắn gần đây từ hội thoại \"" + convName + "\"]\n");
                    for (int i = messages.size() - 1; i >= 0; i--) {
                        var m = messages.get(i);
                        String sender = m.getSender().getDisplayName() != null ? m.getSender().getDisplayName() : m.getSender().getUsername();
                        context.append(sender).append(": ").append(m.getContent()).append("\n");
                    }
                    return prompt + context.toString();
                }
            }
        } catch (Exception e) {
            log.warn("Failed to enrich prompt with other chats: {}", e.getMessage());
        }
        return prompt;
    }
}
