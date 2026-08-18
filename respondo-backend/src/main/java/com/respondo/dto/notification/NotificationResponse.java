package com.respondo.dto.notification;

import com.respondo.enums.NotificationType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
@AllArgsConstructor
public class NotificationResponse {
    private Long id;
    private NotificationType type;
    private String message;
    private Long relatedIncidentId;
    private boolean read;
    private LocalDateTime createdAt;
}
