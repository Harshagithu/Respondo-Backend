package com.respondo.dto.responder;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AssignmentResponseRequest {

    @NotNull(message = "accepted is required")
    private Boolean accepted;

    @Size(max = 500)
    private String notes;
}
