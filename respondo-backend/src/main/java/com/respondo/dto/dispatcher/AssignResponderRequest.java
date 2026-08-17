package com.respondo.dto.dispatcher;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AssignResponderRequest {

    @NotNull(message = "Responder is required")
    private Long responderId;

    @Size(max = 500)
    private String notes;
}
