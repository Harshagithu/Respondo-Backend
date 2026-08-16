package com.respondo.service;

import com.respondo.dto.auth.AuthResponse;
import com.respondo.dto.auth.LoginRequest;
import com.respondo.dto.auth.RegisterRequest;
import com.respondo.dto.auth.RegisterResponse;
import com.respondo.entity.User;
import com.respondo.enums.Role;
import com.respondo.exception.DuplicateResourceException;
import com.respondo.repository.UserRepository;
import com.respondo.security.JwtService;
import com.respondo.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    @Transactional
    public RegisterResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException("An account with this email already exists");
        }

        // Role is force-set to CITIZEN here — RegisterRequest has no role
        // field at all, so there is nothing for a caller to override.
        User user = User.builder()
                .fullName(request.getFullName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .phone(request.getPhone())
                .role(Role.CITIZEN)
                .active(true)
                .build();

        User saved = userRepository.save(user);

        return RegisterResponse.builder()
                .userId(saved.getId())
                .fullName(saved.getFullName())
                .email(saved.getEmail())
                .role(saved.getRole())
                .message("Account created. Please log in.")
                .build();
    }

    public AuthResponse login(LoginRequest request) {
        // Delegates to DaoAuthenticationProvider, which uses
        // CustomUserDetailsService + PasswordEncoder under the hood.
        // Throws BadCredentialsException (handled by GlobalExceptionHandler)
        // on bad email/password or a deactivated account.
        UsernamePasswordAuthenticationToken authToken =
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword());
        var authentication = authenticationManager.authenticate(authToken);

        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        String token = jwtService.generateToken(principal);

        return AuthResponse.builder()
                .token(token)
                .userId(principal.getId())
                .fullName(principal.getUser().getFullName())
                .email(principal.getUsername())
                .role(principal.getUser().getRole())
                .build();
    }
}
