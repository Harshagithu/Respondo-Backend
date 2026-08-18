package com.respondo.dto.admin;

import com.respondo.enums.Role;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

/**
 * role=RESPONDER is deliberately rejected in AdminService — granting
 * that role always goes through the responder application approval
 * flow instead, which also creates/updates the Responder profile row
 * that this generic endpoint has no way to set up correctly.
 */
@Getter
@Setter
public class UserRoleChangeRequest {

    @NotNull(message = "role is required")
    private Role role;
}
