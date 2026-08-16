package com.respondo.dto.auth;

import com.respondo.enums.Role;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

/**
 * Returned only from /api/auth/login (Section 13: JWT is generated at
 * login, not at registration).
 */
@Getter
@Builder
@AllArgsConstructor
public class AuthResponse {
    private String token;
    private Long userId;
    private String fullName;
    private String email;
    private Role role;
}
