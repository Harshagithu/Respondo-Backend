package com.respondo.dto.incident;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/**
 * Deliberately has no status/priority/citizen fields — those are always
 * server-assigned (status = REPORTED, citizen = the authenticated
 * principal, priority = null until dispatcher verification).
 */
@Getter
@Setter
public class IncidentCreateRequest {

    @NotBlank(message = "Title is required")
    @Size(max = 150)
    private String title;

    @NotBlank(message = "Description is required")
    @Size(max = 2000)
    private String description;

    @NotBlank(message = "Location is required")
    @Size(max = 300)
    private String location;

    @NotNull(message = "Category is required")
    private Long categoryId;

    @Min(value = 1, message = "Affected people count must be at least 1")
    private int affectedPeopleCount = 1;
}
