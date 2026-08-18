package com.respondo.dto.admin;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ResponderTeamRequest {

    @NotBlank(message = "Team name is required")
    @Size(max = 120)
    private String name;

    @Size(max = 500)
    private String description;
}
