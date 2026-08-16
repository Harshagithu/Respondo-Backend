package com.respondo.service;

import com.respondo.dto.incident.IncidentCategoryResponse;
import com.respondo.dto.incident.IncidentCreateRequest;
import com.respondo.dto.incident.IncidentResponse;
import com.respondo.dto.incident.IncidentStatusHistoryResponse;
import com.respondo.entity.Incident;
import com.respondo.entity.IncidentCategory;
import com.respondo.entity.IncidentStatusHistory;
import com.respondo.entity.User;
import com.respondo.enums.IncidentStatus;
import com.respondo.enums.Role;
import com.respondo.exception.ForbiddenException;
import com.respondo.exception.ResourceNotFoundException;
import com.respondo.repository.IncidentCategoryRepository;
import com.respondo.repository.IncidentRepository;
import com.respondo.repository.IncidentStatusHistoryRepository;
import com.respondo.repository.UserRepository;
import com.respondo.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class IncidentService {

    private final IncidentRepository incidentRepository;
    private final IncidentCategoryRepository categoryRepository;
    private final UserRepository userRepository;
    private final IncidentStatusHistoryRepository historyRepository;
    private final IncidentWorkflowService workflowService;

    @Transactional
    public IncidentResponse createIncident(UserPrincipal principal, IncidentCreateRequest request) {
        User citizen = userRepository.findById(principal.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        IncidentCategory category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category not found: " + request.getCategoryId()));

        Incident incident = Incident.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .location(request.getLocation())
                .citizen(citizen)
                .category(category)
                .status(IncidentStatus.REPORTED)
                .affectedPeopleCount(request.getAffectedPeopleCount())
                .build();

        Incident saved = incidentRepository.save(incident);

        // No "from" status for the very first row — the incident didn't
        // transition into REPORTED, it was created there.
        workflowService.recordHistory(saved, null, IncidentStatus.REPORTED, citizen, "Incident reported by citizen");

        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<IncidentResponse> getMyIncidents(UserPrincipal principal) {
        return incidentRepository.findByCitizenIdOrderByCreatedAtDesc(principal.getId())
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public IncidentResponse getIncidentById(UserPrincipal principal, Long id) {
        Incident incident = findIncidentOrThrow(id);
        assertViewable(principal, incident);
        return toResponse(incident);
    }

    @Transactional(readOnly = true)
    public List<IncidentStatusHistoryResponse> getIncidentHistory(UserPrincipal principal, Long id) {
        Incident incident = findIncidentOrThrow(id);
        assertViewable(principal, incident);
        return historyRepository.findByIncidentIdOrderByChangedAtAsc(id)
                .stream()
                .map(this::toHistoryResponse)
                .toList();
    }

    public List<IncidentCategoryResponse> listCategories() {
        return categoryRepository.findAll()
                .stream()
                .map(c -> new IncidentCategoryResponse(c.getId(), c.getName(), c.getDescription()))
                .toList();
    }

    private Incident findIncidentOrThrow(Long id) {
        return incidentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Incident not found: " + id));
    }

    /**
     * Section 14: "A citizen requesting another citizen's incident must
     * receive FORBIDDEN." Dispatcher/Admin get full visibility. Responder
     * visibility narrows to "assigned to them only" once Assignment
     * lookups exist (Phase 5) — until then, RESPONDER never reaches this
     * path anyway because /api/incidents/** doesn't grant that role
     * access to /api/responder/** incident views.
     */
    private void assertViewable(UserPrincipal principal, Incident incident) {
        boolean isCitizen = principal.getUser().getRole() == Role.CITIZEN;
        boolean isOwner = incident.getCitizen().getId().equals(principal.getId());

        if (isCitizen && !isOwner) {
            throw new ForbiddenException("You are not allowed to access this incident");
        }
    }

    private IncidentResponse toResponse(Incident i) {
        return IncidentResponse.builder()
                .id(i.getId())
                .title(i.getTitle())
                .description(i.getDescription())
                .location(i.getLocation())
                .status(i.getStatus())
                .priority(i.getPriority())
                .affectedPeopleCount(i.getAffectedPeopleCount())
                .locationRiskLevel(i.getLocationRiskLevel())
                .categoryId(i.getCategory().getId())
                .categoryName(i.getCategory().getName())
                .citizenId(i.getCitizen().getId())
                .citizenName(i.getCitizen().getFullName())
                .verifiedByName(i.getVerifiedBy() != null ? i.getVerifiedBy().getFullName() : null)
                .verifiedAt(i.getVerifiedAt())
                .verificationRemarks(i.getVerificationRemarks())
                .resolvedByName(i.getResolvedBy() != null ? i.getResolvedBy().getFullName() : null)
                .resolvedAt(i.getResolvedAt())
                .resolutionNotes(i.getResolutionNotes())
                .createdAt(i.getCreatedAt())
                .updatedAt(i.getUpdatedAt())
                .build();
    }

    private IncidentStatusHistoryResponse toHistoryResponse(IncidentStatusHistory h) {
        return IncidentStatusHistoryResponse.builder()
                .fromStatus(h.getFromStatus())
                .toStatus(h.getToStatus())
                .changedByName(h.getChangedBy() != null ? h.getChangedBy().getFullName() : "System")
                .changedAt(h.getChangedAt())
                .remarks(h.getRemarks())
                .build();
    }
}
