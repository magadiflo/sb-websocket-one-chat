package dev.magadiflo.app.service;

import dev.magadiflo.app.document.ChatMessage;

import java.util.List;

public interface ChatMessageService {
    List<ChatMessage> findChatMessages(String senderId, String recipientId);

    ChatMessage save(ChatMessage chatMessage);
}
