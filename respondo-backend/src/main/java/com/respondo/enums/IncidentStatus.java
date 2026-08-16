package com.respondo.enums;

/**
 * Full incident lifecycle. Transitions between these are validated in
 * IncidentWorkflowService (Phase 3) — this enum only defines the states.
 */
public enum IncidentStatus {
    REPORTED,
    UNDER_REVIEW,
    VERIFIED,
    PRIORITIZED,
    ASSIGNED,
    RESPONDER_ACCEPTED,
    EN_ROUTE,
    ON_SCENE,
    IN_PROGRESS,
    RESOLVED,
    CLOSED
}
