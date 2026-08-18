package com.respondo.controller;

import com.respondo.dto.admin.*;
import com.respondo.dto.incident.IncidentResponse;
import com.respondo.security.UserPrincipal;
import com.respondo.service.AdminService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;

    @GetMapping("/dashboard")
    public ResponseEntity<DashboardResponse> dashboard() {
        return ResponseEntity.ok(adminService.getDashboard());
    }

    @GetMapping("/users")
    public ResponseEntity<List<UserResponse>> users() {
        return ResponseEntity.ok(adminService.listUsers());
    }

    @PutMapping("/users/{id}/status")
    public ResponseEntity<UserResponse> changeStatus(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id,
            @Valid @RequestBody UserStatusChangeRequest request
    ) {
        return ResponseEntity.ok(adminService.changeUserStatus(principal, id, request));
    }

    @PutMapping("/users/{id}/role")
    public ResponseEntity<UserResponse> changeRole(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id,
            @Valid @RequestBody UserRoleChangeRequest request
    ) {
        return ResponseEntity.ok(adminService.changeUserRole(principal, id, request));
    }

    @GetMapping("/responders")
    public ResponseEntity<List<ResponderAdminResponse>> responders() {
        return ResponseEntity.ok(adminService.listResponders());
    }

    @GetMapping("/responders/applications")
    public ResponseEntity<List<ResponderAdminResponse>> applications() {
        return ResponseEntity.ok(adminService.listResponderApplications());
    }

    @PutMapping("/responders/{id}/approve")
    public ResponseEntity<ResponderAdminResponse> approve(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id
    ) {
        return ResponseEntity.ok(adminService.approveResponder(principal, id));
    }

    @PutMapping("/responders/{id}/reject")
    public ResponseEntity<ResponderAdminResponse> reject(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id
    ) {
        return ResponseEntity.ok(adminService.rejectResponder(principal, id));
    }

    @PutMapping("/responders/{id}/suspend")
    public ResponseEntity<ResponderAdminResponse> suspend(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id
    ) {
        return ResponseEntity.ok(adminService.suspendResponder(principal, id));
    }

    @PutMapping("/responders/{id}/team")
    public ResponseEntity<ResponderAdminResponse> assignTeam(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id,
            @Valid @RequestBody AssignTeamRequest request
    ) {
        return ResponseEntity.ok(adminService.assignResponderTeam(principal, id, request));
    }

    @GetMapping("/dispatchers")
    public ResponseEntity<List<UserResponse>> dispatchers() {
        return ResponseEntity.ok(adminService.listDispatchers());
    }

    @PostMapping("/dispatchers")
    public ResponseEntity<UserResponse> createDispatcher(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody CreateDispatcherRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(adminService.createDispatcher(principal, request));
    }

    @GetMapping("/teams")
    public ResponseEntity<List<ResponderTeamResponse>> teams() {
        return ResponseEntity.ok(adminService.listTeams());
    }

    @PostMapping("/teams")
    public ResponseEntity<ResponderTeamResponse> createTeam(@Valid @RequestBody ResponderTeamRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(adminService.createTeam(request));
    }

    @GetMapping("/audit-logs")
    public ResponseEntity<List<AuditLogResponse>> auditLogs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(adminService.getAuditLogs(page, size));
    }

    /**
     * Section 5: "view all incidents" — not in the spec's literal
     * endpoint list (which only gives dispatcher an active-queue view),
     * added here the same way /api/incidents/categories was in Phase 3.
     */
    @GetMapping("/incidents")
    public ResponseEntity<List<IncidentResponse>> incidents() {
        return ResponseEntity.ok(adminService.listAllIncidents());
    }
}
