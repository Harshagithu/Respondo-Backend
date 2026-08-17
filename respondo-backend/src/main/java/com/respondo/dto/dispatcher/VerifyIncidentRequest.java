package com.respondo.dto.dispatcher;

import com.respondo.enums.LocationRiskLevel;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class VerifyIncidentRequest {

    @NotNull(message = "Location risk level is required")
    private LocationRiskLevel locationRiskLevel;

    @Size(max = 1000)
    private String verificationRemarks;
}
