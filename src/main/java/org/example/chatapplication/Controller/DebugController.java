package org.example.chatapplication.Controller;

import lombok.RequiredArgsConstructor;
import org.example.chatapplication.Model.Entity.ChatMessage;
import org.example.chatapplication.Repository.ChatMessageRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/debug")
@RequiredArgsConstructor
public class DebugController {
    private final ChatMessageRepository chatMessageRepository;

    @GetMapping("/messages")
    public List<ChatMessage> getMessages() {
        return chatMessageRepository.findAll(PageRequest.of(0, 10)).getContent();
    }
}
