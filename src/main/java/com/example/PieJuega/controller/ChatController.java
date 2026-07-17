package com.example.PieJuega.controller;

import com.example.PieJuega.dto.request.CreateChatRoomRequestDTO;
import com.example.PieJuega.dto.request.MarkChatReadRequestDTO;
import com.example.PieJuega.dto.request.SendChatMessageRequestDTO;
import com.example.PieJuega.dto.response.ChatConversationResponseDTO;
import com.example.PieJuega.dto.response.ChatMessageResponseDTO;
import com.example.PieJuega.dto.response.ChatMessagesPageResponseDTO;
import com.example.PieJuega.dto.response.ChatRoomDetailsResponseDTO;
import com.example.PieJuega.security.UserDetailsImpl;
import com.example.PieJuega.service.ChatService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    @GetMapping("/conversations")
    public List<ChatConversationResponseDTO> conversations(
            @AuthenticationPrincipal UserDetailsImpl user
    ) {
        return chatService.getConversations(user.getId());
    }

    @PostMapping("/rooms")
    public ResponseEntity<ChatConversationResponseDTO> createRoom(
            @AuthenticationPrincipal UserDetailsImpl user,
            @Valid @RequestBody CreateChatRoomRequestDTO request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(chatService.createRoom(user.getId(), request));
    }

    @GetMapping("/rooms/{roomId}")
    public ChatRoomDetailsResponseDTO roomDetails(
            @AuthenticationPrincipal UserDetailsImpl user,
            @PathVariable Long roomId
    ) {
        return chatService.getRoomDetails(roomId, user.getId());
    }

    @GetMapping("/rooms/{roomId}/messages")
    public ChatMessagesPageResponseDTO messages(
            @AuthenticationPrincipal UserDetailsImpl user,
            @PathVariable Long roomId,
            @RequestParam(required = false) Long beforeId,
            @RequestParam(defaultValue = "50") int size
    ) {
        return chatService.getMessages(roomId, user.getId(), beforeId, size);
    }

    @PostMapping("/rooms/{roomId}/messages")
    public ResponseEntity<ChatMessageResponseDTO> sendMessage(
            @AuthenticationPrincipal UserDetailsImpl user,
            @PathVariable Long roomId,
            @Valid @RequestBody SendChatMessageRequestDTO request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(chatService.sendMessage(roomId, user.getId(), request));
    }

    @PutMapping("/rooms/{roomId}/read")
    public ResponseEntity<Void> markAsRead(
            @AuthenticationPrincipal UserDetailsImpl user,
            @PathVariable Long roomId,
            @RequestBody(required = false) MarkChatReadRequestDTO request
    ) {
        chatService.markAsRead(
                roomId,
                user.getId(),
                request == null ? null : request.lastMessageId()
        );
        return ResponseEntity.noContent().build();
    }
}
