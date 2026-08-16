package com.respondo.entity;

import com.respondo.common.BaseEntity;
import com.respondo.enums.ResponderApplicationStatus;
import com.respondo.enums.ResponderAvailability;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Field-worker profile for a User whose application to become a
 * responder has been submitted. A row exists as soon as someone applies
 * (applicationStatus = PENDING); the linked User only gets Role.RESPONDER
 * once an admin sets applicationStatus = APPROVED.
 */
@Entity
@Table(name = "responders")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Responder extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "responder_team_id")
    private ResponderTeam responderTeam;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private ResponderApplicationStatus applicationStatus = ResponderApplicationStatus.PENDING;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private ResponderAvailability availability = ResponderAvailability.OFF_DUTY;

    private LocalDateTime appliedAt;

    private LocalDateTime approvedAt;

    @Column(length = 500)
    private String skills;
}
