package com.example.PieJuega.security;

import com.example.PieJuega.repository.ChatRoomMemberRepository;
import com.example.PieJuega.repository.FootballTeamRepository;
import com.example.PieJuega.service.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.security.Principal;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
@RequiredArgsConstructor
public class ChatWebSocketInterceptor implements ChannelInterceptor {

    private static final Pattern ROOM_TOPIC = Pattern.compile("^/topic/chat/rooms/(\\d+)$");
    private static final Pattern USER_TOPIC = Pattern.compile("^/topic/chat/users/(\\d+)$");
    private static final Pattern NOTIFICATION_TOPIC =
            Pattern.compile("^/topic/notifications/users/(\\d+)$");

    private final JwtService jwtService;
    private final UserDetailsServiceImpl userDetailsService;
    private final ChatRoomMemberRepository memberRepository;
    private final FootballTeamRepository teamRepository;

    @Override
    public Message<?> preSend(@NonNull Message<?> message, @NonNull MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(
                message,
                StompHeaderAccessor.class
        );
        if (accessor == null) {
            return message;
        }

        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
            authenticate(accessor);
        } else if (StompCommand.SUBSCRIBE.equals(accessor.getCommand())) {
            authorizeSubscription(accessor);
        }

        return message;
    }

    private void authenticate(StompHeaderAccessor accessor) {
        String header = accessor.getFirstNativeHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ")) {
            throw new MessagingException("Token de acceso requerido");
        }

        String token = header.substring(7);
        if (!jwtService.isTokenValid(token)) {
            throw new MessagingException("Token de acceso inválido");
        }

        Long userId = jwtService.extractUserId(token);
        UserDetailsImpl user = (UserDetailsImpl) userDetailsService.loadUserById(userId);
        if (!user.isEnabled()) {
            throw new MessagingException("Cuenta suspendida");
        }
        accessor.setUser(new ChatPrincipal(user.getId(), user.getUsername()));
    }

    private void authorizeSubscription(StompHeaderAccessor accessor) {
        Long userId = authenticatedUserId(accessor.getUser());
        if (userId == null) {
            throw new MessagingException("Sesión de chat no autenticada");
        }

        String destination = accessor.getDestination();
        if (destination == null) {
            throw new MessagingException("Destino de suscripción inválido");
        }

        Matcher roomMatcher = ROOM_TOPIC.matcher(destination);
        if (roomMatcher.matches()) {
            Long roomId = Long.valueOf(roomMatcher.group(1));
            if (teamRepository.existsByChatRoom_IdAndActiveFalse(roomId)
                    || !memberRepository.existsByRoom_IdAndUser_Id(roomId, userId)) {
                throw new MessagingException("No tienes acceso a esta conversación");
            }
            return;
        }

        Matcher userMatcher = USER_TOPIC.matcher(destination);
        if (userMatcher.matches()
                && Long.valueOf(userMatcher.group(1)).equals(userId)) {
            return;
        }

        Matcher notificationMatcher = NOTIFICATION_TOPIC.matcher(destination);
        if (notificationMatcher.matches()
                && Long.valueOf(notificationMatcher.group(1)).equals(userId)) {
            return;
        }

        throw new MessagingException("Suscripción no permitida");
    }

    private Long authenticatedUserId(Principal principal) {
        if (principal instanceof ChatPrincipal chatPrincipal) {
            return chatPrincipal.userId();
        }

        if (principal instanceof Authentication authentication
                && authentication.getPrincipal() instanceof UserDetailsImpl user) {
            return user.getId();
        }

        return null;
    }
}
