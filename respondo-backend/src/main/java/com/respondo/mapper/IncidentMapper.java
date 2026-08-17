package com.respondo.mapper;

import com.respondo.dto.incident.IncidentResponse;
import com.respondo.dto.incident.IncidentStatusHistoryResponse;
import com.respondo.entity.Incident;
import com.respondo.entity.IncidentStatusHistory;

/**
 * Shared Incident -> DTO mapping. Extracted out of IncidentService in
 * Phase 4 so DispatcherService (and ResponderService/AdminService in
 * later phases) can reuse the exact same mapping instead of each
 * duplicating it — one incident response shape, one place it's built.
 */
public final class IncidentMapper {

    private IncidentMapper() {
    }

    public static IncidentResponse toResponse(Incident i) {
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

    public static IncidentStatusHistoryResponse toHistoryResponse(IncidentStatusHistory h) {
        return IncidentStatusHistoryResponse.builder()
                .fromStatus(h.getFromStatus())
                .toStatus(h.getToStatus())
                .changedByName(h.getChangedBy() != null ? h.getChangedBy().getFullName() : "System")
                .changedAt(h.getChangedAt())
                .remarks(h.getRemarks())
                .build();
    }
}
