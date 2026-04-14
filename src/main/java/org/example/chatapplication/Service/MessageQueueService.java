package org.example.chatapplication.Service;

import lombok.RequiredArgsConstructor;
import org.example.chatapplication.ChatMessageCreatedEvent;
import org.example.chatapplication.Model.Entity.ChatMessage;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MessageQueueService {
    private final ApplicationEventPublisher eventPublisher;

    public void publishMessageCreated(ChatMessage message) {
        eventPublisher.publishEvent(new ChatMessageCreatedEvent(message.getId(), message.getConversation().getId(), message.getSender().getId()));
    }
}
