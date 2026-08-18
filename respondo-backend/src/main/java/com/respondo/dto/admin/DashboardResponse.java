package com.respondo.dto.admin;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
@AllArgsConstructor
public class DashboardResponse {
    private long totalUsers;
    private long totalCitizens;
    private long totalDispatchers;
    private long totalResponders;
    private long activeIncidents;
    private long resolvedIncidents;
    private long pendingResponderApplications;
    private long availableResponders;
    private List<AuditLogResponse> recentAuditLogs;
}
