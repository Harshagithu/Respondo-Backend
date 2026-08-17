package com.respondo.controller;

import com.respondo.dto.dispatcher.AssignResponderRequest;
import com.respondo.dto.dispatcher.AvailableResponderResponse;
import com.respondo.dto.dispatcher.PriorityChangeRequest;
import com.respondo.dto.dispatcher.VerifyIncidentRequest;
import com.respondo.dto.incident.IncidentResponse;
import com.respondo.security.UserPrincipal;
import com.respondo.service.DispatcherService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/dispatcher")
@RequiredArgsConstructor
public class DispatcherController {

    private final DispatcherService dispatcherService;

    @GetMapping("/incidents")
    public ResponseEntity<List<IncidentResponse>> queue() {
        return ResponseEntity.ok(dispatcherService.getQueue());
    }

    @PutMapping("/incidents/{id}/verify")
    public ResponseEntity<IncidentResponse> verify(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id,
            @Valid @RequestBody VerifyIncidentRequest request
    ) {
        return ResponseEntity.ok(dispatcherService.verifyIncident(principal, id, request));
    }

    @PutMapping("/incidents/{id}/priority")
    public ResponseEntity<IncidentResponse> overridePriority(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id,
            @Valid @RequestBody PriorityChangeRequest request
    ) {
        return ResponseEntity.ok(dispatcherService.overridePriority(principal, id, request));
    }

    @PutMapping("/incidents/{id}/assign")
    public ResponseEntity<IncidentResponse> assign(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id,
            @Valid @RequestBody AssignResponderRequest request
    ) {
        return ResponseEntity.ok(dispatcherService.assignResponder(principal, id, request));
    }

    @GetMapping("/responders/available")
    public ResponseEntity<List<AvailableResponderResponse>> availableResponders() {
        return ResponseEntity.ok(dispatcherService.getAvailableResponders());
    }
}
