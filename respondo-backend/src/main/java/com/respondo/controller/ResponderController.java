package com.respondo.controller;

import com.respondo.dto.responder.AssignmentResponseRequest;
import com.respondo.dto.responder.ResolveIncidentRequest;
import com.respondo.dto.responder.ResponderIncidentResponse;
import com.respondo.dto.responder.StatusUpdateRequest;
import com.respondo.security.UserPrincipal;
import com.respondo.service.ResponderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/responder")
@RequiredArgsConstructor
public class ResponderController {

    private final ResponderService responderService;

    @GetMapping("/incidents")
    public ResponseEntity<List<ResponderIncidentResponse>> myIncidents(@AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(responderService.getMyIncidents(principal));
    }

    @GetMapping("/assignments")
    public ResponseEntity<List<ResponderIncidentResponse>> assignmentHistory(@AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(responderService.getAssignmentHistory(principal));
    }

    @PutMapping("/incidents/{id}/assignment-response")
    public ResponseEntity<ResponderIncidentResponse> respond(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id,
            @Valid @RequestBody AssignmentResponseRequest request
    ) {
        return ResponseEntity.ok(responderService.respondToAssignment(principal, id, request));
    }

    @PutMapping("/incidents/{id}/status")
    public ResponseEntity<ResponderIncidentResponse> updateStatus(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id,
            @Valid @RequestBody StatusUpdateRequest request
    ) {
        return ResponseEntity.ok(responderService.updateStatus(principal, id, request));
    }

    @PutMapping("/incidents/{id}/resolve")
    public ResponseEntity<ResponderIncidentResponse> resolve(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id,
            @Valid @RequestBody ResolveIncidentRequest request
    ) {
        return ResponseEntity.ok(responderService.resolve(principal, id, request));
    }
}
