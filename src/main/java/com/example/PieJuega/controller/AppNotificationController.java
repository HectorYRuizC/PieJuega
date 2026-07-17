package com.example.PieJuega.controller;

import com.example.PieJuega.dto.response.AppNotificationResponseDTO;
import com.example.PieJuega.dto.response.UnreadCountResponseDTO;
import com.example.PieJuega.security.UserDetailsImpl;
import com.example.PieJuega.service.AppNotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class AppNotificationController {

    private final AppNotificationService notificationService;

    @GetMapping
    public List<AppNotificationResponseDTO> notifications(
            @AuthenticationPrincipal UserDetailsImpl user,
            @RequestParam(defaultValue = "50") int limit
    ) {
        return notificationService.getNotifications(user.getId(), limit);
    }

    @GetMapping("/unread-count")
    public UnreadCountResponseDTO unreadCount(@AuthenticationPrincipal UserDetailsImpl user) {
        return new UnreadCountResponseDTO(notificationService.getUnreadCount(user.getId()));
    }

    @PatchMapping("/{notificationId}/read")
    public AppNotificationResponseDTO markRead(
            @AuthenticationPrincipal UserDetailsImpl user,
            @PathVariable Long notificationId
    ) {
        return notificationService.markRead(notificationId, user.getId());
    }

    @PatchMapping("/read-all")
    public ResponseEntity<Void> markAllRead(@AuthenticationPrincipal UserDetailsImpl user) {
        notificationService.markAllRead(user.getId());
        return ResponseEntity.noContent().build();
    }
}
