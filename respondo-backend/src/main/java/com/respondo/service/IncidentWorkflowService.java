package com.respondo.service;

import com.respondo.entity.Incident;
import com.respondo.entity.IncidentStatusHistory;
import com.respondo.entity.User;
import com.respondo.enums.IncidentStatus;
import com.respondo.exception.InvalidStateTransitionException;
import com.respondo.repository.IncidentStatusHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Set;

import static com.respondo.enums.IncidentStatus.*;

/**
 * The single place that knows which incident status transitions are
 * legal (Section 8). Every future module (dispatcher verify/prioritize/
 * assign, responder accept/reject/status/resolve) calls
 * {@link #applyTransition} instead of calling {@code incident.setStatus(...)}
 * directly, so the state machine can never be bypassed or drift out of
 * sync between modules.
 *
 * ASSIGNED has two legal exits: RESPONDER_ACCEPTED (responder accepts)
 * and PRIORITIZED (responder rejects — Section 10: "the incident must
 * then be available for reassignment"). CLOSED has no exits; it's terminal.
 */
@Service
@RequiredArgsConstructor
public class IncidentWorkflowService {

    private final IncidentStatusHistoryRepository historyRepository;

    private static final Map<IncidentStatus, Set<IncidentStatus>> TRANSITIONS = Map.ofEntries(
            Map.entry(REPORTED, Set.of(UNDER_REVIEW)),
            Map.entry(UNDER_REVIEW, Set.of(VERIFIED)),
            Map.entry(VERIFIED, Set.of(PRIORITIZED)),
            Map.entry(PRIORITIZED, Set.of(ASSIGNED)),
            Map.entry(ASSIGNED, Set.of(RESPONDER_ACCEPTED, PRIORITIZED)),
            Map.entry(RESPONDER_ACCEPTED, Set.of(EN_ROUTE)),
            Map.entry(EN_ROUTE, Set.of(ON_SCENE)),
            Map.entry(ON_SCENE, Set.of(IN_PROGRESS)),
            Map.entry(IN_PROGRESS, Set.of(RESOLVED)),
            Map.entry(RESOLVED, Set.of(CLOSED)),
            Map.entry(CLOSED, Set.of())
    );

    /**
     * Validates the transition, mutates the incident's status, and writes
     * the corresponding history row — always together, so a status can
     * never change without leaving an audit trail.
     */
    public void applyTransition(Incident incident, IncidentStatus newStatus, User changedBy, String remarks) {
        IncidentStatus current = incident.getStatus();
        Set<IncidentStatus> allowed = TRANSITIONS.getOrDefault(current, Set.of());

        if (!allowed.contains(newStatus)) {
            throw new InvalidStateTransitionException(
                    "Cannot move incident from " + current + " to " + newStatus);
        }

        incident.setStatus(newStatus);
        recordHistory(incident, current, newStatus, changedBy, remarks);
    }

    /**
     * Records a history row without a transition check — used once, for
     * the very first REPORTED entry, which has no "from" status to
     * validate against.
     */
    public void recordHistory(Incident incident, IncidentStatus from, IncidentStatus to, User changedBy, String remarks) {
        IncidentStatusHistory history = IncidentStatusHistory.builder()
                .incident(incident)
                .fromStatus(from)
                .toStatus(to)
                .changedBy(changedBy)
                .changedAt(LocalDateTime.now())
                .remarks(remarks)
                .build();
        historyRepository.save(history);
    }
}
