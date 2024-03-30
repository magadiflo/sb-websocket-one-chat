package dev.magadiflo.app.service;

import dev.magadiflo.app.document.ChatRoom;
import dev.magadiflo.app.repository.ChatRoomRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

@RequiredArgsConstructor
@Service
public class ChatRoomServiceImpl implements ChatRoomService {

    private final ChatRoomRepository chatRoomRepository;

    @Override
    public Optional<String> getChatRoomId(String senderId, String recipientId, boolean createNewRoomIfNotExists) {
        return this.chatRoomRepository.findBySenderIdAndRecipientId(senderId, recipientId)
                .map(ChatRoom::getId)
                .or(() -> {
                    if (createNewRoomIfNotExists) {
                        String chatId = this.createChatId(senderId, recipientId);
                        return Optional.of(chatId);
                    }
                    return Optional.empty();
                });
    }

    private String createChatId(String senderId, String recipientId) {
        String chatId = String.format("%s_%s", senderId, recipientId);

        // Para crear dos salas de chat (chat room), uno para el sender y otro para el recipient
        ChatRoom senderRecipient = ChatRoom.builder()
                .chatId(chatId)
                .senderId(senderId)
                .recipientId(recipientId)
                .build();

        ChatRoom recipientSender = ChatRoom.builder()
                .chatId(chatId)
                .senderId(recipientId)
                .recipientId(senderId)
                .build();

        this.chatRoomRepository.save(senderRecipient);
        this.chatRoomRepository.save(recipientSender);

        return chatId;
    }
}