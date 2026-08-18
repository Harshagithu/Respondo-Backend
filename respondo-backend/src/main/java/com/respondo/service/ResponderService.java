package com.respondo.service;

import com.respondo.dto.responder.AssignmentResponseRequest;
import com.respondo.dto.responder.ResolveIncidentRequest;
import com.respondo.dto.responder.ResponderApplicationRequest;
import com.respondo.dto.responder.ResponderIncidentResponse;
import com.respondo.dto.responder.StatusUpdateRequest;
import com.respondo.entity.Assignment;
import com.respondo.entity.Incident;
import com.respondo.entity.Responder;
import com.respondo.entity.User;
import com.respondo.enums.AssignmentStatus;
import com.respondo.enums.IncidentStatus;
import com.respondo.enums.NotificationType;
import com.respondo.enums.ResponderApplicationStatus;
import com.respondo.enums.ResponderAvailability;
import com.respondo.enums.Role;
import com.respondo.exception.DuplicateResourceException;
import com.respondo.exception.ForbiddenException;
import com.respondo.exception.InvalidStateTransitionException;
import com.respondo.exception.ResourceNotFoundException;
import com.respondo.mapper.IncidentMapper;
import com.respondo.repository.AssignmentRepository;
import com.respondo.repository.IncidentRepository;
import com.respondo.repository.ResponderRepository;
import com.respondo.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ResponderService {

    /**
     * The only statuses a responder may move an incident to via the
     * generic status-update endpoint. RESOLVED is intentionally excluded
     * — that always goes through resolve(), which forces resolution
     * notes and updates the incident's resolvedBy/resolvedAt.
     */
    private static final Set<IncidentStatus> STATUS_UPDATE_TARGETS =
            Set.of(IncidentStatus.EN_ROUTE, IncidentStatus.ON_SCENE, IncidentStatus.IN_PROGRESS);

    private final IncidentRepository incidentRepository;
    private final ResponderRepository responderRepository;
    private final AssignmentRepository assignmentRepository;
    private final IncidentWorkflowService workflowService;
    private final AuditLogService auditLogService;
    private final NotificationService notificationService;

    /**
     * Section 6: "A public user may apply to become a responder." This
     * was a gap left open in Phase 5 — nothing let a citizen create the
     * PENDING Responder row that Phase 7's admin approval flow reviews.
     * Closing it here rather than leaving it as a permanent SQL-only
     * testing step.
     */
    @Transactional
    public String applyToBecomeResponder(UserPrincipal principal, ResponderApplicationRequest request) {
        if (principal.getUser().getRole() != Role.CITIZEN) {
            throw new ForbiddenException("Only citizen accounts can apply to become a responder");
        }
        if (responderRepository.findByUserId(principal.getId()).isPresent()) {
            throw new DuplicateResourceException("You have already applied to become a responder");
        }

        Responder responder = Responder.builder()
                .user(principal.getUser())
                .applicationStatus(ResponderApplicationStatus.PENDING)
                .availability(ResponderAvailability.OFF_DUTY)
                .appliedAt(LocalDateTime.now())
                .skills(request != null ? request.getSkills() : null)
                .build();
        responderRepository.save(responder);

        return "Application submitted. An admin will review it.";
    }

    @Transactional(readOnly = true)
    public List<ResponderIncidentResponse> getMyIncidents(UserPrincipal principal) {
        Responder responder = findResponderOrThrow(principal.getId());

        return assignmentRepository.findByResponderIdOrderByAssignedAtDesc(responder.getId())
                .stream()
                .filter(a -> a.getStatus() == AssignmentStatus.PENDING || a.getStatus() == AssignmentStatus.ACCEPTED)
                .map(this::toResponse)
                .toList();
    }

    /**
     * Section 6/22's "assignment history" capability — not in the spec's
     * literal endpoint list, added the same way /api/incidents/categories
     * was in Phase 3: an obvious, narrowly-scoped extension of an existing
     * module rather than a new one.
     */
    @Transactional(readOnly = true)
    public List<ResponderIncidentResponse> getAssignmentHistory(UserPrincipal principal) {
        Responder responder = findResponderOrThrow(principal.getId());

        return assignmentRepository.findByResponderIdOrderByAssignedAtDesc(responder.getId())
                .stream()
                .map(this::toResponse)
                .toList();
    }

    /**
     * Section 10: accept -> RESPONDER_ACCEPTED. Reject -> responder goes
     * back to AVAILABLE and the incident returns to PRIORITIZED so a
     * dispatcher can reassign it.
     */
    @Transactional
    public ResponderIncidentResponse respondToAssignment(UserPrincipal principal, Long incidentId, AssignmentResponseRequest request) {
        Responder responder = findResponderOrThrow(principal.getId());
        Incident incident = findIncidentOrThrow(incidentId);
        Assignment assignment = findOwnedAssignmentOrThrow(incidentId, AssignmentStatus.PENDING, responder);
        User responderUser = principal.getUser();

        assignment.setRespondedAt(LocalDateTime.now());
        if (request.getNotes() != null && !request.getNotes().isBlank()) {
            assignment.setNotes(request.getNotes());
        }

        if (Boolean.TRUE.equals(request.getAccepted())) {
            assignment.setStatus(AssignmentStatus.ACCEPTED);
            workflowService.applyTransition(incident, IncidentStatus.RESPONDER_ACCEPTED, responderUser, "Responder accepted assignment");
            auditLogService.record(responderUser, "RESPONDER_ACCEPTED", "Incident", incident.getId(), assignment.getNotes());
            notificationService.notify(incident.getCitizen(), NotificationType.STATUS_UPDATE,
                    "A responder has accepted your incident and is on the way.", incident);
        } else {
            assignment.setStatus(AssignmentStatus.REJECTED);
            responder.setAvailability(ResponderAvailability.AVAILABLE);
            responderRepository.save(responder);
            workflowService.applyTransition(incident, IncidentStatus.PRIORITIZED, responderUser, "Responder rejected assignment — returned to pool");
            auditLogService.record(responderUser, "RESPONDER_REJECTED", "Incident", incident.getId(), assignment.getNotes());
        }

        assignmentRepository.save(assignment);
        Incident savedIncident = incidentRepository.save(incident);

        return toResponse(assignment, savedIncident);
    }

    @Transactional
    public ResponderIncidentResponse updateStatus(UserPrincipal principal, Long incidentId, StatusUpdateRequest request) {
        Responder responder = findResponderOrThrow(principal.getId());
        Incident incident = findIncidentOrThrow(incidentId);
        Assignment assignment = findOwnedAssignmentOrThrow(incidentId, AssignmentStatus.ACCEPTED, responder);
        User responderUser = principal.getUser();

        if (!STATUS_UPDATE_TARGETS.contains(request.getStatus())) {
            throw new InvalidStateTransitionException(
                    "This endpoint only accepts EN_ROUTE, ON_SCENE, or IN_PROGRESS — use /resolve to mark an incident RESOLVED");
        }

        workflowService.applyTransition(incident, request.getStatus(), responderUser, request.getRemarks());
        Incident saved = incidentRepository.save(incident);

        auditLogService.record(responderUser, "STATUS_UPDATE", "Incident", incident.getId(),
                "Status changed to " + request.getStatus());
        notificationService.notify(saved.getCitizen(), NotificationType.STATUS_UPDATE,
                "Your incident status has been updated to " + request.getStatus() + ".", saved);

        return toResponse(assignment, saved);
    }

    /**
     * Section 11: only the responder assigned to the incident can resolve
     * it. On success: assignment COMPLETED, responder back to AVAILABLE,
     * incident RESOLVED with resolver/notes/timestamp recorded.
     */
    @Transactional
    public ResponderIncidentResponse resolve(UserPrincipal principal, Long incidentId, ResolveIncidentRequest request) {
        Responder responder = findResponderOrThrow(principal.getId());
        Incident incident = findIncidentOrThrow(incidentId);
        Assignment assignment = findOwnedAssignmentOrThrow(incidentId, AssignmentStatus.ACCEPTED, responder);
        User responderUser = principal.getUser();

        if (incident.getStatus() != IncidentStatus.IN_PROGRESS) {
            throw new InvalidStateTransitionException(
                    "Incident must be IN_PROGRESS to resolve (current status: " + incident.getStatus() + ")");
        }

        workflowService.applyTransition(incident, IncidentStatus.RESOLVED, responderUser, "Incident resolved by responder");
        incident.setResolvedBy(responderUser);
        incident.setResolvedAt(LocalDateTime.now());
        incident.setResolutionNotes(request.getResolutionNotes());
        Incident savedIncident = incidentRepository.save(incident);

        assignment.setStatus(AssignmentStatus.COMPLETED);
        assignmentRepository.save(assignment);

        responder.setAvailability(ResponderAvailability.AVAILABLE);
        responderRepository.save(responder);

        auditLogService.record(responderUser, "INCIDENT_RESOLVED", "Incident", incident.getId(), request.getResolutionNotes());
        notificationService.notify(savedIncident.getCitizen(), NotificationType.INCIDENT_RESOLVED,
                "Your incident has been resolved.", savedIncident);

        return toResponse(assignment, savedIncident);
    }

    private Responder findResponderOrThrow(Long userId) {
        return responderRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("No responder profile for this account"));
    }

    private Incident findIncidentOrThrow(Long id) {
        return incidentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Incident not found: " + id));
    }

    /**
     * Section 14: "A responder trying to update another responder's
     * incident must receive FORBIDDEN." Looks up the assignment by
     * incident + required status, then explicitly verifies it belongs to
     * the calling responder rather than trusting the incident id alone.
     */
    private Assignment findOwnedAssignmentOrThrow(Long incidentId, AssignmentStatus requiredStatus, Responder responder) {
        Assignment assignment = assignmentRepository.findByIncidentIdAndStatus(incidentId, requiredStatus)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No " + requiredStatus + " assignment found for incident " + incidentId));

        if (!assignment.getResponder().getId().equals(responder.getId())) {
            throw new ForbiddenException("This incident is not assigned to you");
        }

        return assignment;
    }

    private ResponderIncidentResponse toResponse(Assignment a) {
        return toResponse(a, a.getIncident());
    }

    private ResponderIncidentResponse toResponse(Assignment a, Incident incident) {
        return ResponderIncidentResponse.builder()
                .assignmentId(a.getId())
                .assignmentStatus(a.getStatus())
                .assignedAt(a.getAssignedAt())
                .respondedAt(a.getRespondedAt())
                .notes(a.getNotes())
                .incident(IncidentMapper.toResponse(incident))
                .build();
    }
}
