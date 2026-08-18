package com.respondo.service;

import com.respondo.dto.notification.NotificationResponse;
import com.respondo.entity.Incident;
import com.respondo.entity.Notification;
import com.respondo.entity.User;
import com.respondo.enums.NotificationType;
import com.respondo.exception.ForbiddenException;
import com.respondo.exception.ResourceNotFoundException;
import com.respondo.repository.NotificationRepository;
import com.respondo.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;

    /**
     * Called by other services (dispatcher/responder modules), not
     * exposed as its own controller endpoint — notifications are always
     * a side effect of some other workflow action, never created
     * directly by a client request.
     */
    @Transactional
    public void notify(User recipient, NotificationType type, String message, Incident relatedIncident) {
        Notification notification = Notification.builder()
                .recipient(recipient)
                .type(type)
                .message(message)
                .relatedIncident(relatedIncident)
                .read(false)
                .build();
        notificationRepository.save(notification);
    }

    @Transactional(readOnly=true)
    public List<NotificationResponse> getNotifications(UserPrincipal principal) {
        return notificationRepository.findByRecipientIdOrderByCreatedAtDesc(principal.getId())
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public void markAsRead(UserPrincipal principal, Long notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found: " + notificationId));

        if (!notification.getRecipient().getId().equals(principal.getId())) {
            throw new ForbiddenException("This notification does not belong to you");
        }

        notification.setRead(true);
        notificationRepository.save(notification);
    }

    @Transactional(readOnly = true)
    public long getUnreadCount(UserPrincipal principal) {
        return notificationRepository.countByRecipientIdAndReadFalse(principal.getId());
    }

    private NotificationResponse toResponse(Notification n) {
        return NotificationResponse.builder()
                .id(n.getId())
                .type(n.getType())
                .message(n.getMessage())
                .relatedIncidentId(n.getRelatedIncident() != null ? n.getRelatedIncident().getId() : null)
                .read(n.isRead())
                .createdAt(n.getCreatedAt())
                .build();
    }
}
