package com.respondo.dto.responder;

import com.respondo.enums.IncidentStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/**
 * status is restricted server-side to EN_ROUTE / ON_SCENE / IN_PROGRESS —
 * see ResponderService. RESOLVED is deliberately not reachable through
 * this endpoint; that requires ResolveIncidentRequest instead, which
 * forces resolution notes to be provided.
 */
@Getter
@Setter
public class StatusUpdateRequest {

    @NotNull(message = "Status is required")
    private IncidentStatus status;

    @Size(max = 500)
    private String remarks;
}
