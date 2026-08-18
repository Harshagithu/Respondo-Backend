package com.respondo.service;

import com.respondo.dto.dispatcher.AssignResponderRequest;
import com.respondo.dto.dispatcher.AvailableResponderResponse;
import com.respondo.dto.dispatcher.PriorityChangeRequest;
import com.respondo.dto.dispatcher.VerifyIncidentRequest;
import com.respondo.dto.incident.IncidentResponse;
import com.respondo.entity.Assignment;
import com.respondo.entity.Incident;
import com.respondo.entity.Responder;
import com.respondo.entity.User;
import com.respondo.enums.AssignmentStatus;
import com.respondo.enums.IncidentStatus;
import com.respondo.enums.NotificationType;
import com.respondo.enums.Priority;
import com.respondo.enums.ResponderApplicationStatus;
import com.respondo.enums.ResponderAvailability;
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

@Service
@RequiredArgsConstructor
public class DispatcherService {

    private static final List<IncidentStatus> QUEUE_STATUSES = List.of(
            IncidentStatus.REPORTED,
            IncidentStatus.UNDER_REVIEW,
            IncidentStatus.VERIFIED,
            IncidentStatus.PRIORITIZED,
            IncidentStatus.ASSIGNED
    );

    private final IncidentRepository incidentRepository;
    private final ResponderRepository responderRepository;
    private final AssignmentRepository assignmentRepository;
    private final IncidentWorkflowService workflowService;
    private final PriorityCalculationService priorityCalculationService;
    private final AuditLogService auditLogService;
    private final NotificationService notificationService;

    @Transactional(readOnly = true)
    public List<IncidentResponse> getQueue() {
        return incidentRepository.findByStatusInOrderByCreatedAtAsc(QUEUE_STATUSES)
                .stream()
                .map(IncidentMapper::toResponse)
                .toList();
    }

    /**
     * REPORTED/UNDER_REVIEW -> VERIFIED -> (auto priority calc) -> PRIORITIZED,
     * all in one call. The workflow diagram (Section 8) shows verification
     * flowing straight into automatic prioritization with no manual step in
     * between, so this endpoint performs both transitions atomically rather
     * than requiring the frontend to make two separate calls.
     */
    @Transactional
    public IncidentResponse verifyIncident(UserPrincipal principal, Long incidentId, VerifyIncidentRequest request) {
        Incident incident = findIncidentOrThrow(incidentId);
        User dispatcher = principal.getUser();

        if (incident.getStatus() != IncidentStatus.REPORTED && incident.getStatus() != IncidentStatus.UNDER_REVIEW) {
            throw new InvalidStateTransitionException(
                    "Incident must be REPORTED or UNDER_REVIEW to verify (current status: " + incident.getStatus() + ")");
        }

        if (incident.getStatus() == IncidentStatus.REPORTED) {
            workflowService.applyTransition(incident, IncidentStatus.UNDER_REVIEW, dispatcher, "Dispatcher started review");
        }

        incident.setLocationRiskLevel(request.getLocationRiskLevel());
        incident.setVerificationRemarks(request.getVerificationRemarks());
        incident.setVerifiedBy(dispatcher);
        incident.setVerifiedAt(LocalDateTime.now());

        workflowService.applyTransition(incident, IncidentStatus.VERIFIED, dispatcher, "Incident verified by dispatcher");
        auditLogService.record(dispatcher, "INCIDENT_VERIFIED", "Incident", incident.getId(), request.getVerificationRemarks());

        Priority priority = priorityCalculationService.calculate(incident);
        incident.setPriority(priority);
        workflowService.applyTransition(incident, IncidentStatus.PRIORITIZED, dispatcher,
                "Auto-calculated priority: " + priority);
        auditLogService.record(dispatcher, "PRIORITY_CALCULATED", "Incident", incident.getId(),
                "Auto-calculated priority: " + priority);

        Incident saved = incidentRepository.save(incident);

        notificationService.notify(saved.getCitizen(), NotificationType.INCIDENT_VERIFIED,
                "Your incident has been verified and prioritized as " + priority + ".", saved);

        return IncidentMapper.toResponse(saved);
    }

    /**
     * Section 9: manual priority override. Only meaningful once an incident
     * has an automatically calculated priority to override, and before
     * resolution makes priority moot — so PRIORITIZED or ASSIGNED only.
     */
    @Transactional
    public IncidentResponse overridePriority(UserPrincipal principal, Long incidentId, PriorityChangeRequest request) {
        Incident incident = findIncidentOrThrow(incidentId);

        if (incident.getStatus() != IncidentStatus.PRIORITIZED && incident.getStatus() != IncidentStatus.ASSIGNED) {
            throw new InvalidStateTransitionException(
                    "Priority can only be overridden while the incident is PRIORITIZED or ASSIGNED (current status: "
                            + incident.getStatus() + ")");
        }

        Priority previous = incident.getPriority();
        incident.setPriority(request.getPriority());
        Incident saved = incidentRepository.save(incident);

        auditLogService.record(principal.getUser(), "PRIORITY_OVERRIDDEN", "Incident", incidentId,
                previous + " -> " + request.getPriority() + " (reason: " + request.getReason() + ")");

        return IncidentMapper.toResponse(saved);
    }

    /**
     * Section 10: dispatcher assigns exactly one AVAILABLE, APPROVED
     * responder to a PRIORITIZED incident. Creates a new Assignment row
     * (append-only history — see IncidentWorkflowService's javadoc),
     * flips the responder to BUSY, and moves the incident to ASSIGNED.
     */
    @Transactional
    public IncidentResponse assignResponder(UserPrincipal principal, Long incidentId, AssignResponderRequest request) {
        Incident incident = findIncidentOrThrow(incidentId);
        User dispatcher = principal.getUser();

        if (incident.getStatus() != IncidentStatus.PRIORITIZED) {
            throw new InvalidStateTransitionException(
                    "Only a PRIORITIZED incident can be assigned (current status: " + incident.getStatus() + ")");
        }

        Responder responder = responderRepository.findById(request.getResponderId())
                .orElseThrow(() -> new ResourceNotFoundException("Responder not found: " + request.getResponderId()));

        if (responder.getApplicationStatus() != ResponderApplicationStatus.APPROVED) {
            throw new ForbiddenException("This responder's application has not been approved");
        }

        if (responder.getAvailability() != ResponderAvailability.AVAILABLE) {
            throw new InvalidStateTransitionException(
                    "Only an AVAILABLE responder can be assigned (current: " + responder.getAvailability() + ")");
        }

        Assignment assignment = Assignment.builder()
                .incident(incident)
                .responder(responder)
                .assignedBy(dispatcher)
                .assignedAt(LocalDateTime.now())
                .status(AssignmentStatus.PENDING)
                .notes(request.getNotes())
                .build();
        assignmentRepository.save(assignment);

        responder.setAvailability(ResponderAvailability.BUSY);
        responderRepository.save(responder);

        workflowService.applyTransition(incident, IncidentStatus.ASSIGNED, dispatcher,
                "Assigned to responder #" + responder.getId());
        Incident saved = incidentRepository.save(incident);

        auditLogService.record(dispatcher, "RESPONDER_ASSIGNED", "Incident", incident.getId(),
                "Assigned to responder #" + responder.getId());
        notificationService.notify(responder.getUser(), NotificationType.INCIDENT_ASSIGNED,
                "You have been assigned to incident #" + incident.getId() + ".", saved);

        return IncidentMapper.toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<AvailableResponderResponse> getAvailableResponders() {
        return responderRepository.findByAvailability(ResponderAvailability.AVAILABLE)
                .stream()
                .filter(r -> r.getApplicationStatus() == ResponderApplicationStatus.APPROVED)
                .map(r -> new AvailableResponderResponse(
                        r.getId(),
                        r.getUser().getId(),
                        r.getUser().getFullName(),
                        r.getResponderTeam() != null ? r.getResponderTeam().getName() : null,
                        r.getAvailability()
                ))
                .toList();
    }

    private Incident findIncidentOrThrow(Long id) {
        return incidentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Incident not found: " + id));
    }
}
