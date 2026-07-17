package com.example.PieJuega.service;

import com.example.PieJuega.model.AppNotification;
import com.example.PieJuega.model.User;
import com.example.PieJuega.repository.AppNotificationRepository;
import com.example.PieJuega.repository.UserRepository;
import com.example.PieJuega.util.NotificationType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AppNotificationServiceTests {

    @Mock
    private AppNotificationRepository notificationRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @Mock
    private PushNotificationService pushNotificationService;

    @Test
    void coalescesUnreadMessagesFromTheSameRoom() {
        User recipient = User.builder().id(7L).username("Jhoan").build();
        AppNotification existing = AppNotification.builder()
                .id(12L)
                .recipient(recipient)
                .type(NotificationType.CHAT_MESSAGE)
                .sourceId(4L)
                .title("Equipo")
                .body("Mensaje anterior")
                .build();
        when(notificationRepository
                .findFirstByRecipient_IdAndTypeAndSourceIdAndReadAtIsNull(
                        7L,
                        NotificationType.CHAT_MESSAGE,
                        4L
                ))
                .thenReturn(Optional.of(existing));
        when(notificationRepository.save(any(AppNotification.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        AppNotificationService service = new AppNotificationService(
                notificationRepository,
                userRepository,
                messagingTemplate,
                pushNotificationService
        );

        var result = service.notifyUser(
                recipient,
                NotificationType.CHAT_MESSAGE,
                "Equipo PieJuega",
                "Laura: Nos vemos a las 7",
                "/teamChat/4",
                4L
        );

        assertEquals(12L, result.id());
        assertEquals("Laura: Nos vemos a las 7", result.body());
        assertFalse(result.read());
        verify(messagingTemplate).convertAndSend(
                eq("/topic/notifications/users/7"),
                eq(result)
        );
        verify(pushNotificationService).sendToUser(7L, result);
    }
}
