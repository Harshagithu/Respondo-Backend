package com.respondo.entity;

import com.respondo.common.BaseEntity;
import com.respondo.enums.AssignmentStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * One row per dispatch attempt of an incident to a responder. An incident
 * that gets rejected and reassigned ends up with multiple Assignment rows
 * (one PENDING/REJECTED, a later one ACCEPTED) — this preserves full
 * dispatch history instead of overwriting a single "current responder"
 * field on Incident.
 */
@Entity
@Table(name = "assignments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Assignment extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "incident_id", nullable = false)
    private Incident incident;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "responder_id", nullable = false)
    private Responder responder;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "assigned_by", nullable = false)
    private User assignedBy;

    @Column(nullable = false)
    private LocalDateTime assignedAt;

    private LocalDateTime respondedAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    @Builder.Default
    private AssignmentStatus status = AssignmentStatus.PENDING;

    @Column(length = 1000)
    private String notes;
}
