package com.example.PieJuega.config;

import com.example.PieJuega.model.ChatMessage;
import com.example.PieJuega.model.ChatRoom;
import com.example.PieJuega.model.ChatRoomMember;
import com.example.PieJuega.model.User;
import com.example.PieJuega.repository.ChatMessageRepository;
import com.example.PieJuega.repository.ChatRoomMemberRepository;
import com.example.PieJuega.repository.ChatRoomRepository;
import com.example.PieJuega.repository.UserRepository;
import com.example.PieJuega.util.ChatMessageType;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@Profile("dev")
@RequiredArgsConstructor
public class DevChatDataInitializer implements ApplicationRunner {

    private static final String COMMUNITY_ROOM = "Comunidad PieJuega";

    private final UserRepository userRepository;
    private final ChatRoomRepository roomRepository;
    private final ChatRoomMemberRepository memberRepository;
    private final ChatMessageRepository messageRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        List<User> users = userRepository.findAll();
        if (users.isEmpty()) {
            return;
        }

        User creator = users.get(0);
        ChatRoom room = roomRepository.findByNameIgnoreCase(COMMUNITY_ROOM)
                .orElseGet(() -> roomRepository.save(ChatRoom.builder()
                        .name(COMMUNITY_ROOM)
                        .category("Comunidad")
                        .createdBy(creator)
                        .build()));

        for (User user : users) {
            if (!memberRepository.existsByRoom_IdAndUser_Id(room.getId(), user.getId())) {
                memberRepository.save(ChatRoomMember.builder()
                        .room(room)
                        .user(user)
                        .build());
            }
        }

        if (!messageRepository.existsByRoom_Id(room.getId())) {
            messageRepository.save(ChatMessage.builder()
                    .room(room)
                    .sender(creator)
                    .content("Bienvenidos al chat de PieJuega.")
                    .messageType(ChatMessageType.TEXT)
                    .build());
            messageRepository.save(ChatMessage.builder()
                    .room(room)
                    .sender(creator)
                    .content("Aquí podemos organizar partidos y compartir novedades.")
                    .messageType(ChatMessageType.TEXT)
                    .build());
        }
    }
}
