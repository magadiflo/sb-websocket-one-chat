package dev.magadiflo.app.controller;

import dev.magadiflo.app.document.ChatMessage;
import dev.magadiflo.app.dto.ChatNotification;
import dev.magadiflo.app.service.ChatMessageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

@RequiredArgsConstructor
@Controller
public class ChatController {

    private final SimpMessagingTemplate simpMessagingTemplate;
    private final ChatMessageService chatMessageService;

    @MessageMapping("/chat")
    public void processMessage(@Payload ChatMessage chatMessage) {
        ChatMessage chatMessageDB = this.chatMessageService.save(chatMessage);
        ChatNotification payload = new ChatNotification(
                chatMessageDB.getId(),
                chatMessageDB.getSenderId(),
                chatMessageDB.getRecipientId(),
                chatMessageDB.getContent()
        );

        // Queremos enviar el payload a la cola de abajo. Ejemplo de cómo sería la cola: magadiflo/queue/messages y se
        // envía el payload, donde el getRecipientId(), para nuestro ejemplo es magadiflo.
        // Luego magadiflo, se subscribirá a la cola magadiflo/queue/messages
        this.simpMessagingTemplate.convertAndSendToUser(chatMessage.getRecipientId(), "/queue/messages", payload);
    }

    @GetMapping("/messages/{senderId}/{recipientId}")
    public ResponseEntity<List<ChatMessage>> findChatMessages(@PathVariable String senderId, @PathVariable String recipientId) {
        return ResponseEntity.ok(this.chatMessageService.findChatMessages(senderId, recipientId));
    }
}
