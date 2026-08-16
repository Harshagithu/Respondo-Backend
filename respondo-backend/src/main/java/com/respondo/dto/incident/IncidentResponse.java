package com.respondo.dto.incident;

import com.respondo.enums.IncidentStatus;
import com.respondo.enums.LocationRiskLevel;
import com.respondo.enums.Priority;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
@AllArgsConstructor
public class IncidentResponse {
    private Long id;
    private String title;
    private String description;
    private String location;
    private IncidentStatus status;
    private Priority priority;
    private int affectedPeopleCount;
    private LocationRiskLevel locationRiskLevel;

    private Long categoryId;
    private String categoryName;

    private Long citizenId;
    private String citizenName;

    private String verifiedByName;
    private LocalDateTime verifiedAt;
    private String verificationRemarks;

    private String resolvedByName;
    private LocalDateTime resolvedAt;
    private String resolutionNotes;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
