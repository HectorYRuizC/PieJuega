package com.example.PieJuega.service;

import com.example.PieJuega.dto.request.CreateChatRoomRequestDTO;
import com.example.PieJuega.dto.request.SendChatMessageRequestDTO;
import com.example.PieJuega.dto.response.ChatConversationResponseDTO;
import com.example.PieJuega.dto.response.ChatEventResponseDTO;
import com.example.PieJuega.dto.response.ChatMemberResponseDTO;
import com.example.PieJuega.dto.response.ChatMessageResponseDTO;
import com.example.PieJuega.dto.response.ChatMessagesPageResponseDTO;
import com.example.PieJuega.dto.response.ChatRoomDetailsResponseDTO;
import com.example.PieJuega.exception.ChatAccessDeniedException;
import com.example.PieJuega.exception.ResourceNotFoundException;
import com.example.PieJuega.model.ChatMessage;
import com.example.PieJuega.model.ChatRoom;
import com.example.PieJuega.model.ChatRoomMember;
import com.example.PieJuega.model.User;
import com.example.PieJuega.repository.ChatMessageRepository;
import com.example.PieJuega.repository.ChatRoomMemberRepository;
import com.example.PieJuega.repository.ChatRoomRepository;
import com.example.PieJuega.repository.FootballTeamRepository;
import com.example.PieJuega.repository.UserRepository;
import com.example.PieJuega.util.ChatMessageType;
import com.example.PieJuega.util.NotificationType;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ChatService {

    private static final int MAX_PAGE_SIZE = 100;

    private final ChatRoomRepository roomRepository;
    private final ChatRoomMemberRepository memberRepository;
    private final ChatMessageRepository messageRepository;
    private final UserRepository userRepository;
    private final FootballTeamRepository teamRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final AppNotificationService notificationService;

    @Transactional(readOnly = true)
    public List<ChatConversationResponseDTO> getConversations(Long userId) {
        Set<Long> archivedRoomIds = teamRepository.findArchivedChatRoomIds();
        return memberRepository.findByUser_IdOrderByRoom_UpdatedAtDesc(userId)
                .stream()
                .filter(member -> !archivedRoomIds.contains(member.getRoom().getId()))
                .map(member -> toConversation(member.getRoom(), member))
                .toList();
    }

    @Transactional(readOnly = true)
    public ChatRoomDetailsResponseDTO getRoomDetails(Long roomId, Long userId) {
        requireMembership(roomId, userId);
        ChatRoom room = getRoom(roomId);

        List<ChatMemberResponseDTO> members = memberRepository
                .findByRoom_IdOrderByJoinedAtAsc(roomId)
                .stream()
                .map(member -> new ChatMemberResponseDTO(
                        member.getUser().getId(),
                        member.getUser().getUsername(),
                        member.getUser().getPhotoUrl()
                ))
                .toList();

        return new ChatRoomDetailsResponseDTO(
                room.getId(),
                room.getName(),
                room.getCategory(),
                room.getImageUrl(),
                members
        );
    }

    @Transactional(readOnly = true)
    public ChatMessagesPageResponseDTO getMessages(
            Long roomId,
            Long userId,
            Long beforeId,
            int requestedSize
    ) {
        ChatRoomMember membership = requireMembership(roomId, userId);
        int size = Math.max(1, Math.min(requestedSize, MAX_PAGE_SIZE));
        PageRequest pageRequest = PageRequest.of(0, size);

        Page<ChatMessage> page = beforeId == null
                ? messageRepository.findByRoom_IdOrderByIdDesc(roomId, pageRequest)
                : messageRepository.findByRoom_IdAndIdLessThanOrderByIdDesc(
                        roomId,
                        beforeId,
                        pageRequest
                );

        List<ChatMessageResponseDTO> messages = new ArrayList<>(
                page.getContent().stream()
                        .map(message -> toMessage(message, membership))
                        .toList()
        );
        Collections.reverse(messages);

        Long nextBeforeId = page.hasNext() && !messages.isEmpty()
                ? messages.get(0).id()
                : null;

        return new ChatMessagesPageResponseDTO(messages, page.hasNext(), nextBeforeId);
    }

    @Transactional
    public ChatMessageResponseDTO sendMessage(
            Long roomId,
            Long userId,
            SendChatMessageRequestDTO request
    ) {
        ChatRoomMember senderMembership = requireMembership(roomId, userId);
        ChatRoom room = senderMembership.getRoom();
        User sender = getUser(userId);

        validateMessage(request);
        String content = normalizedContent(request);

        ChatMessage saved = messageRepository.save(ChatMessage.builder()
                .room(room)
                .sender(sender)
                .content(content)
                .messageType(request.messageType())
                .mediaUrl(blankToNull(request.mediaUrl()))
                .build());

        senderMembership.setLastReadMessageId(saved.getId());
        memberRepository.save(senderMembership);
        room.setUpdatedAt(LocalDateTime.now());
        roomRepository.save(room);

        ChatMessageResponseDTO response = toMessage(saved, senderMembership);
        publishMessage(room, response);
        notifyMembers(room, sender, content);
        return response;
    }

    @Transactional
    public void markAsRead(Long roomId, Long userId, Long requestedMessageId) {
        ChatRoomMember membership = requireMembership(roomId, userId);
        Long lastMessageId = requestedMessageId;

        if (lastMessageId == null) {
            lastMessageId = messageRepository.findFirstByRoom_IdOrderByIdDesc(roomId)
                    .map(ChatMessage::getId)
                    .orElse(0L);
        } else {
            ChatMessage message = messageRepository.findById(lastMessageId)
                    .filter(candidate -> candidate.getRoom().getId().equals(roomId))
                    .orElseThrow(() -> new ResourceNotFoundException("Mensaje no encontrado"));
            lastMessageId = message.getId();
        }

        if (lastMessageId > membership.getLastReadMessageId()) {
            membership.setLastReadMessageId(lastMessageId);
            memberRepository.save(membership);
        }
    }

    @Transactional
    public ChatConversationResponseDTO createRoom(
            Long creatorId,
            CreateChatRoomRequestDTO request
    ) {
        Set<Long> memberIds = new LinkedHashSet<>();
        memberIds.add(creatorId);
        if (request.memberIds() != null) {
            memberIds.addAll(request.memberIds());
        }

        ChatRoom room = createRoomForMembers(
                creatorId,
                request.name(),
                request.category(),
                request.imageUrl(),
                memberIds
        );

        ChatRoomMember creatorMembership = memberRepository
                .findByRoom_IdAndUser_Id(room.getId(), creatorId)
                .orElseThrow();
        return toConversation(room, creatorMembership);
    }

    @Transactional
    public ChatRoom createTeamRoom(
            Long creatorId,
            String teamName,
            String imageUrl,
            Set<Long> memberIds
    ) {
        return createRoomForMembers(
                creatorId,
                teamName,
                "Equipo",
                imageUrl,
                memberIds
        );
    }

    private ChatRoom createRoomForMembers(
            Long creatorId,
            String name,
            String category,
            String imageUrl,
            Set<Long> requestedMemberIds
    ) {
        User creator = getUser(creatorId);
        ChatRoom room = roomRepository.save(ChatRoom.builder()
                .name(name.trim())
                .category(category.trim())
                .imageUrl(blankToNull(imageUrl))
                .createdBy(creator)
                .build());

        Set<Long> memberIds = new LinkedHashSet<>(requestedMemberIds);
        memberIds.add(creatorId);

        List<User> users = userRepository.findAllById(memberIds);
        if (users.size() != memberIds.size()) {
            throw new ResourceNotFoundException("Uno o más participantes no existen");
        }

        users.forEach(user -> memberRepository.save(ChatRoomMember.builder()
                .room(room)
                .user(user)
                .build()));
        publishConversationCreated(room);
        return room;
    }

    @Transactional(readOnly = true)
    public boolean isMember(Long roomId, Long userId) {
        return memberRepository.existsByRoom_IdAndUser_Id(roomId, userId);
    }

    private ChatRoomMember requireMembership(Long roomId, Long userId) {
        if (teamRepository.existsByChatRoom_IdAndActiveFalse(roomId)) {
            throw new ChatAccessDeniedException("Este equipo está archivado");
        }
        return memberRepository.findByRoom_IdAndUser_Id(roomId, userId)
                .orElseThrow(() -> new ChatAccessDeniedException(
                        "No tienes acceso a esta conversación"
                ));
    }

    private ChatRoom getRoom(Long roomId) {
        return roomRepository.findById(roomId)
                .orElseThrow(() -> new ResourceNotFoundException("Conversación no encontrada"));
    }

    private User getUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));
    }

    private ChatConversationResponseDTO toConversation(
            ChatRoom room,
            ChatRoomMember membership
    ) {
        ChatMessage lastMessage = messageRepository
                .findFirstByRoom_IdOrderByIdDesc(room.getId())
                .orElse(null);
        long unreadCount = messageRepository.countByRoom_IdAndIdGreaterThan(
                room.getId(),
                membership.getLastReadMessageId()
        );

        return new ChatConversationResponseDTO(
                room.getId(),
                room.getName(),
                room.getCategory(),
                room.getImageUrl(),
                memberRepository.countByRoom_Id(room.getId()),
                lastMessage == null ? "Aún no hay mensajes" : lastMessage.getContent(),
                lastMessage == null ? null : lastMessage.getSender().getUsername(),
                lastMessage == null ? room.getCreatedAt() : lastMessage.getSentAt(),
                unreadCount
        );
    }

    private ChatMessageResponseDTO toMessage(
            ChatMessage message,
            ChatRoomMember membership
    ) {
        boolean read = message.getSender().getId().equals(membership.getUser().getId())
                || message.getId() <= membership.getLastReadMessageId();

        return new ChatMessageResponseDTO(
                message.getId(),
                message.getRoom().getId(),
                message.getSender().getId(),
                message.getSender().getUsername(),
                message.getSender().getPhotoUrl(),
                message.getContent(),
                message.getMessageType(),
                message.getMediaUrl(),
                message.getSentAt(),
                read
        );
    }

    private void publishMessage(ChatRoom room, ChatMessageResponseDTO message) {
        ChatEventResponseDTO roomEvent = new ChatEventResponseDTO(
                "MESSAGE_CREATED",
                room.getId(),
                message,
                null
        );
        messagingTemplate.convertAndSend("/topic/chat/rooms/" + room.getId(), roomEvent);

        for (ChatRoomMember member : memberRepository.findByRoom_IdOrderByJoinedAtAsc(room.getId())) {
            ChatConversationResponseDTO conversation = toConversation(room, member);
            ChatEventResponseDTO userEvent = new ChatEventResponseDTO(
                    "CONVERSATION_UPDATED",
                    room.getId(),
                    message,
                    conversation
            );
            messagingTemplate.convertAndSend(
                    "/topic/chat/users/" + member.getUser().getId(),
                    userEvent
            );
        }
    }

    private void publishConversationCreated(ChatRoom room) {
        for (ChatRoomMember member : memberRepository.findByRoom_IdOrderByJoinedAtAsc(room.getId())) {
            ChatEventResponseDTO event = new ChatEventResponseDTO(
                    "CONVERSATION_CREATED",
                    room.getId(),
                    null,
                    toConversation(room, member)
            );
            messagingTemplate.convertAndSend(
                    "/topic/chat/users/" + member.getUser().getId(),
                    event
            );
        }
    }

    private void notifyMembers(ChatRoom room, User sender, String content) {
        memberRepository.findByRoom_IdOrderByJoinedAtAsc(room.getId()).stream()
                .map(ChatRoomMember::getUser)
                .filter(user -> !user.getId().equals(sender.getId()))
                .forEach(user -> notificationService.notifyUser(
                        user,
                        NotificationType.CHAT_MESSAGE,
                        room.getName(),
                        sender.getUsername() + ": " + content,
                        "/teamChat/" + room.getId(),
                        room.getId()
                ));
    }

    private void validateMessage(SendChatMessageRequestDTO request) {
        if (request.messageType() == ChatMessageType.TEXT
                && (request.content() == null || request.content().isBlank())) {
            throw new IllegalArgumentException("El mensaje no puede estar vacío");
        }

        if ((request.messageType() == ChatMessageType.IMAGE
                || request.messageType() == ChatMessageType.LOCATION)
                && (request.mediaUrl() == null || request.mediaUrl().isBlank())) {
            throw new IllegalArgumentException("El archivo o ubicación es obligatorio");
        }
    }

    private String normalizedContent(SendChatMessageRequestDTO request) {
        if (request.content() != null && !request.content().isBlank()) {
            return request.content().trim();
        }
        return request.messageType() == ChatMessageType.IMAGE ? "Imagen" : "Ubicación";
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
