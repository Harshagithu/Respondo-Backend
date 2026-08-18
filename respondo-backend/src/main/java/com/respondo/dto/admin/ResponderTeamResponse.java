package com.respondo.dto.admin;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ResponderTeamResponse {
    private Long id;
    private String name;
    private String description;
}
