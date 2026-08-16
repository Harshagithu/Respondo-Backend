package com.respondo.controller;

import com.respondo.dto.incident.IncidentCategoryResponse;
import com.respondo.dto.incident.IncidentCreateRequest;
import com.respondo.dto.incident.IncidentResponse;
import com.respondo.dto.incident.IncidentStatusHistoryResponse;
import com.respondo.security.UserPrincipal;
import com.respondo.service.IncidentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/incidents")
@RequiredArgsConstructor
public class IncidentController {

    private final IncidentService incidentService;

    @PostMapping
    public ResponseEntity<IncidentResponse> create(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody IncidentCreateRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(incidentService.createIncident(principal, request));
    }

    @GetMapping("/my")
    public ResponseEntity<List<IncidentResponse>> myIncidents(@AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(incidentService.getMyIncidents(principal));
    }

    @GetMapping("/categories")
    public ResponseEntity<List<IncidentCategoryResponse>> categories() {
        return ResponseEntity.ok(incidentService.listCategories());
    }

    @GetMapping("/{id}")
    public ResponseEntity<IncidentResponse> getById(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id
    ) {
        return ResponseEntity.ok(incidentService.getIncidentById(principal, id));
    }

    @GetMapping("/{id}/history")
    public ResponseEntity<List<IncidentStatusHistoryResponse>> history(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id
    ) {
        return ResponseEntity.ok(incidentService.getIncidentHistory(principal, id));
    }
}
