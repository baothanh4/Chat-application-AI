package org.example.chatapplication.Controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.example.chatapplication.DTO.Request.DeleteMessageRequest;
import org.example.chatapplication.DTO.Request.EditMessageRequest;
import org.example.chatapplication.DTO.Request.ReplyMessageRequest;
import org.example.chatapplication.DTO.Request.SendMessageRequest;
import org.example.chatapplication.DTO.Request.ToggleMessagePinRequest;
import org.example.chatapplication.DTO.Request.UnsendMessageRequest;
import org.example.chatapplication.DTO.Request.UpdateMessageReactionRequest;
import org.example.chatapplication.DTO.Response.ChatMessageResponse;
import org.example.chatapplication.DTO.Response.ConversationMessageEventResponse;
import org.example.chatapplication.DTO.Response.MessageHistoryResponse;
import org.example.chatapplication.Service.ChatMessageService;
import org.example.chatapplication.Service.FileStorageService;
import org.example.chatapplication.Model.Enum.MessageType;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/messages")
@RequiredArgsConstructor
@Validated
public class MessageController {
    private final ChatMessageService chatMessageService;
    private final FileStorageService fileStorageService;
    private final SimpMessagingTemplate messagingTemplate;

    @PostMapping
    ResponseEntity<ChatMessageResponse> sendMessage(@RequestBody @Valid SendMessageRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(chatMessageService.sendMessage(request));
    }

    @PostMapping(value = "/video", consumes = org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE)
    ResponseEntity<ChatMessageResponse> sendVideoMessage(@RequestParam(value = "conversationId", required = false) UUID conversationId,
                                                         @RequestParam("senderId") UUID senderId,
                                                         @RequestParam(value = "recipientId", required = false) UUID recipientId,
                                                         @RequestParam(value = "clientMessageId", required = false) String clientMessageId,
                                                         @RequestParam("file") MultipartFile file) {
        String videoUrl = fileStorageService.storeChatVideo(file);
        SendMessageRequest request = new SendMessageRequest();
        request.setConversationId(conversationId);
        request.setSenderId(senderId);
        request.setRecipientId(recipientId);
        request.setClientMessageId(clientMessageId);
        request.setMessageType(MessageType.VIDEO);
        request.setContent(videoUrl);
        return ResponseEntity.status(HttpStatus.CREATED).body(chatMessageService.sendMessage(request));
    }

    @PostMapping(value = "/image", consumes = org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE)
    ResponseEntity<ChatMessageResponse> sendImageMessage(@RequestParam(value = "conversationId", required = false) UUID conversationId,
                                                         @RequestParam("senderId") UUID senderId,
                                                         @RequestParam(value = "recipientId", required = false) UUID recipientId,
                                                         @RequestParam(value = "clientMessageId", required = false) String clientMessageId,
                                                         @RequestParam("file") MultipartFile file) {
        String imageUrl = fileStorageService.storeChatImage(file);
        SendMessageRequest request = new SendMessageRequest();
        request.setConversationId(conversationId);
        request.setSenderId(senderId);
        request.setRecipientId(recipientId);
        request.setClientMessageId(clientMessageId);
        request.setMessageType(MessageType.IMAGE);
        request.setContent(imageUrl);
        return ResponseEntity.status(HttpStatus.CREATED).body(chatMessageService.sendMessage(request));
    }

    @PostMapping(value = "/file", consumes = org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE)
    ResponseEntity<ChatMessageResponse> sendFileMessage(@RequestParam(value = "conversationId", required = false) UUID conversationId,
                                                        @RequestParam("senderId") UUID senderId,
                                                        @RequestParam(value = "recipientId", required = false) UUID recipientId,
                                                        @RequestParam(value = "clientMessageId", required = false) String clientMessageId,
                                                        @RequestParam("file") MultipartFile file) {
        String fileUrl = fileStorageService.storeChatFile(file);
        SendMessageRequest request = new SendMessageRequest();
        request.setConversationId(conversationId);
        request.setSenderId(senderId);
        request.setRecipientId(recipientId);
        request.setClientMessageId(clientMessageId);
        request.setMessageType(MessageType.FILE);
        request.setContent(fileUrl);
        return ResponseEntity.status(HttpStatus.CREATED).body(chatMessageService.sendMessage(request));
    }

    @PostMapping("/{messageId}/reply")
    ResponseEntity<ChatMessageResponse> replyMessage(@PathVariable UUID messageId,
                                                     @RequestBody @Valid ReplyMessageRequest request) {
        ChatMessageResponse response = chatMessageService.replyMessage(messageId, request);
        publishEvent("MESSAGE_REPLIED", response.getConversationId(), response.getId(), request.getActorUserId(), response);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PatchMapping("/{messageId}/edit")
    ResponseEntity<ChatMessageResponse> editMessage(@PathVariable UUID messageId,
                                                    @RequestBody @Valid EditMessageRequest request) {
        ChatMessageResponse response = chatMessageService.editMessage(messageId, request.getActorUserId(), request.getContent());
        publishEvent("MESSAGE_EDITED", response.getConversationId(), response.getId(), request.getActorUserId(), response);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{messageId}/unsend")
    ResponseEntity<ChatMessageResponse> unsendMessage(@PathVariable UUID messageId,
                                                      @RequestBody @Valid UnsendMessageRequest request) {
        ChatMessageResponse response = chatMessageService.unsendMessage(messageId, request.getActorUserId());
        publishEvent("MESSAGE_UNSENT", response.getConversationId(), response.getId(), request.getActorUserId(), response);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{messageId}")
    ResponseEntity<ChatMessageResponse> deleteMessage(@PathVariable UUID messageId,
                                                      @RequestBody @Valid DeleteMessageRequest request) {
        ChatMessageResponse response = chatMessageService.deleteMessageForEveryone(messageId, request.getActorUserId());
        publishEvent("MESSAGE_DELETED", response.getConversationId(), response.getId(), request.getActorUserId(), response);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{messageId}/pin")
    ResponseEntity<ChatMessageResponse> pinMessage(@PathVariable UUID messageId,
                                                   @RequestBody @Valid ToggleMessagePinRequest request) {
        ChatMessageResponse response = chatMessageService.pinMessage(messageId, request.getActorUserId());
        publishPinEvent("MESSAGE_PINNED", response, request.getActorUserId());
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{messageId}/unpin")
    ResponseEntity<ChatMessageResponse> unpinMessage(@PathVariable UUID messageId,
                                                     @RequestBody @Valid ToggleMessagePinRequest request) {
        ChatMessageResponse response = chatMessageService.unpinMessage(messageId, request.getActorUserId());
        publishPinEvent("MESSAGE_UNPINNED", response, request.getActorUserId());
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{messageId}/reaction")
    ResponseEntity<ChatMessageResponse> reactMessage(@PathVariable UUID messageId,
                                                     @RequestBody @Valid UpdateMessageReactionRequest request) {
        ChatMessageResponse response = chatMessageService.addOrUpdateReaction(messageId, request.getActorUserId(), request.getEmoji());
        publishEvent("MESSAGE_REACTED", response.getConversationId(), response.getId(), request.getActorUserId(), response);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{messageId}/reaction")
    ResponseEntity<ChatMessageResponse> removeReaction(@PathVariable UUID messageId,
                                                       @RequestParam UUID actorUserId) {
        ChatMessageResponse response = chatMessageService.removeReaction(messageId, actorUserId);
        publishEvent("MESSAGE_REACTION_REMOVED", response.getConversationId(), response.getId(), actorUserId, response);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/conversations/{conversationId}/pinned")
    ResponseEntity<List<ChatMessageResponse>> listPinnedMessages(@PathVariable UUID conversationId,
                                                                 @RequestParam UUID actorUserId) {
        return ResponseEntity.ok(chatMessageService.listPinnedMessages(conversationId, actorUserId));
    }

    @GetMapping("/conversations/{conversationId}/media")
    ResponseEntity<MessageHistoryResponse> listConversationMedia(@PathVariable UUID conversationId,
                                                                 @RequestParam UUID actorUserId,
                                                                 @RequestParam MessageType type,
                                                                 @RequestParam(defaultValue = "0") int page,
                                                                 @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(chatMessageService.getConversationMedia(conversationId, actorUserId, type, page, size));
    }

    private void publishPinEvent(String eventType, ChatMessageResponse message, UUID actorUserId) {
        ConversationMessageEventResponse event = new ConversationMessageEventResponse(
                eventType,
                message.getConversationId(),
                message.getId(),
                actorUserId,
                Instant.now(),
                message
        );
        messagingTemplate.convertAndSend("/topic/conversations/" + message.getConversationId() + "/messages.pinned", event);
    }

    private void publishEvent(String eventType, UUID conversationId, UUID messageId, UUID actorUserId, ChatMessageResponse message) {
        ConversationMessageEventResponse event = new ConversationMessageEventResponse(
                eventType,
                conversationId,
                messageId,
                actorUserId,
                Instant.now(),
                message
        );
        messagingTemplate.convertAndSend("/topic/conversations/" + conversationId + "/messages.mutated", event);
    }
}
