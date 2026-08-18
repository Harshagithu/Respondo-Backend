package com.respondo.dto.admin;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AssignTeamRequest {

    @NotNull(message = "teamId is required")
    private Long teamId;
}
