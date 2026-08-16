package com.respondo.entity;

import com.respondo.common.BaseEntity;
import com.respondo.enums.IncidentStatus;
import com.respondo.enums.LocationRiskLevel;
import com.respondo.enums.Priority;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
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
 * The central aggregate of the system. `status` drives the whole
 * workflow (Section 8) and every change to it must additionally create
 * an IncidentStatusHistory row (enforced in the service layer, not here).
 */
@Entity
@Table(
    name = "incidents",
    indexes = {
        @Index(name = "idx_incident_status", columnList = "status"),
        @Index(name = "idx_incident_priority", columnList = "priority"),
        @Index(name = "idx_incident_citizen", columnList = "citizen_id")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Incident extends BaseEntity {

    @Column(nullable = false, length = 150)
    private String title;

    @Column(nullable = false, length = 2000)
    private String description;

    @Column(nullable = false, length = 300)
    private String location;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "citizen_id", nullable = false)
    private User citizen;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    private IncidentCategory category;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 25)
    @Builder.Default
    private IncidentStatus status = IncidentStatus.REPORTED;

    @Enumerated(EnumType.STRING)
    @Column(length = 10)
    private Priority priority;

    @Column(nullable = false)
    @Builder.Default
    private int affectedPeopleCount = 1;

    @Enumerated(EnumType.STRING)
    @Column(length = 10)
    private LocationRiskLevel locationRiskLevel;

    // --- Verification (Phase 4 / Dispatcher) ---
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "verified_by")
    private User verifiedBy;

    private LocalDateTime verifiedAt;

    @Column(length = 1000)
    private String verificationRemarks;

    // --- Resolution (Phase 5 / Responder) ---
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "resolved_by")
    private User resolvedBy;

    private LocalDateTime resolvedAt;

    @Column(length = 1500)
    private String resolutionNotes;
}
