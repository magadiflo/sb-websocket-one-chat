package dev.magadiflo.app.service.impl;

import dev.magadiflo.app.document.ChatMessage;
import dev.magadiflo.app.repository.ChatMessageRepository;
import dev.magadiflo.app.service.ChatMessageService;
import dev.magadiflo.app.service.ChatRoomService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@RequiredArgsConstructor
@Service
public class ChatMessageServiceImpl implements ChatMessageService {

    private final ChatMessageRepository chatMessageRepository;
    private final ChatRoomService chatRoomService;

    @Override
    public List<ChatMessage> findChatMessages(String senderId, String recipientId) {
        return this.chatRoomService.getChatRoomId(senderId, recipientId, false)
                .map(this.chatMessageRepository::findByChatId)
                .orElseGet(ArrayList::new);
    }

    @Override
    public ChatMessage save(ChatMessage chatMessage) {
        String chatId = this.chatRoomService.getChatRoomId(chatMessage.getSenderId(), chatMessage.getRecipientId(), true)
                .orElseThrow();
        chatMessage.setChatId(chatId);
        this.chatMessageRepository.save(chatMessage);
        return chatMessage;
    }
}
