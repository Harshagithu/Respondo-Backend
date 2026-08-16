package com.respondo.dto.auth;

import com.respondo.enums.Role;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

/**
 * Confirms account creation without issuing a token — the caller must
 * still hit /api/auth/login to authenticate, matching the two distinct
 * flows described in the spec.
 */
@Getter
@Builder
@AllArgsConstructor
public class RegisterResponse {
    private Long userId;
    private String fullName;
    private String email;
    private Role role;
    private String message;
}
