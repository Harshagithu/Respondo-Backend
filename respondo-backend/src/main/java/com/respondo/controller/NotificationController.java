package com.respondo.controller;

import com.respondo.dto.notification.NotificationResponse;
import com.respondo.security.UserPrincipal;
import com.respondo.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    public ResponseEntity<List<NotificationResponse>> list(@AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(notificationService.getNotifications(principal));
    }

    @PutMapping("/{id}/read")
    public ResponseEntity<Void> markRead(@AuthenticationPrincipal UserPrincipal principal, @PathVariable Long id) {
        notificationService.markAsRead(principal, id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/unread-count")
    public ResponseEntity<Long> unreadCount(@AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(notificationService.getUnreadCount(principal));
    }
}
