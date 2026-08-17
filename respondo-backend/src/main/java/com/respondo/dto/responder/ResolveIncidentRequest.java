package com.respondo.dto.responder;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ResolveIncidentRequest {

    @NotBlank(message = "Resolution notes are required")
    @Size(max = 1500)
    private String resolutionNotes;
}
