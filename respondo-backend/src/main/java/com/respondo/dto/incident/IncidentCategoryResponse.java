package com.respondo.dto.incident;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class IncidentCategoryResponse {
    private Long id;
    private String name;
    private String description;
}
