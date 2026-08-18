package com.respondo.dto.admin;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserStatusChangeRequest {

    @NotNull(message = "active is required")
    private Boolean active;
}
