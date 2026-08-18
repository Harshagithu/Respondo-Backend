package com.respondo.dto.admin;

import com.respondo.enums.ResponderApplicationStatus;
import com.respondo.enums.ResponderAvailability;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
@AllArgsConstructor
public class ResponderAdminResponse {
    private Long responderId;
    private Long userId;
    private String fullName;
    private String email;
    private ResponderApplicationStatus applicationStatus;
    private ResponderAvailability availability;
    private Long teamId;
    private String teamName;
    private LocalDateTime appliedAt;
    private LocalDateTime approvedAt;
    private String skills;
}
