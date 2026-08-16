package com.respondo.dto.incident;

import com.respondo.enums.IncidentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
@AllArgsConstructor
public class IncidentStatusHistoryResponse {
    private IncidentStatus fromStatus;
    private IncidentStatus toStatus;
    private String changedByName;
    private LocalDateTime changedAt;
    private String remarks;
}
