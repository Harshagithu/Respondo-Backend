package com.respondo.dto.responder;

import com.respondo.dto.incident.IncidentResponse;
import com.respondo.enums.AssignmentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * Wraps IncidentResponse with the responder's own assignment context
 * (which of possibly several historical assignments this is, and its
 * PENDING/ACCEPTED/REJECTED/COMPLETED status) — the frontend needs both
 * to decide whether to show accept/reject controls, status controls, or
 * a read-only history entry.
 */
@Getter
@Builder
@AllArgsConstructor
public class ResponderIncidentResponse {
    private Long assignmentId;
    private AssignmentStatus assignmentStatus;
    private LocalDateTime assignedAt;
    private LocalDateTime respondedAt;
    private String notes;
    private IncidentResponse incident;
}
