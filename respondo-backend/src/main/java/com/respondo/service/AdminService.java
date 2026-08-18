package com.respondo.service;

import com.respondo.dto.admin.*;
import com.respondo.dto.incident.IncidentResponse;
import com.respondo.entity.AuditLog;
import com.respondo.entity.Responder;
import com.respondo.entity.ResponderTeam;
import com.respondo.entity.User;
import com.respondo.enums.IncidentStatus;
import com.respondo.enums.Role;
import com.respondo.enums.ResponderApplicationStatus;
import com.respondo.enums.ResponderAvailability;
import com.respondo.exception.DuplicateResourceException;
import com.respondo.exception.ForbiddenException;
import com.respondo.exception.InvalidStateTransitionException;
import com.respondo.exception.ResourceNotFoundException;
import com.respondo.mapper.IncidentMapper;
import com.respondo.repository.AuditLogRepository;
import com.respondo.repository.IncidentRepository;
import com.respondo.repository.ResponderRepository;
import com.respondo.repository.ResponderTeamRepository;
import com.respondo.repository.UserRepository;
import com.respondo.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminService {

    private static final List<IncidentStatus> ACTIVE_STATUSES = List.of(
            IncidentStatus.REPORTED, IncidentStatus.UNDER_REVIEW, IncidentStatus.VERIFIED,
            IncidentStatus.PRIORITIZED, IncidentStatus.ASSIGNED, IncidentStatus.RESPONDER_ACCEPTED,
            IncidentStatus.EN_ROUTE, IncidentStatus.ON_SCENE, IncidentStatus.IN_PROGRESS
    );
    private static final List<IncidentStatus> RESOLVED_STATUSES = List.of(IncidentStatus.RESOLVED, IncidentStatus.CLOSED);

    private final UserRepository userRepository;
    private final ResponderRepository responderRepository;
    private final ResponderTeamRepository responderTeamRepository;
    private final IncidentRepository incidentRepository;
    private final AuditLogRepository auditLogRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditLogService auditLogService;

    @Transactional(readOnly = true)
    public DashboardResponse getDashboard() {
        List<AuditLogResponse> recent = auditLogRepository.findAllByOrderByCreatedAtDesc(PageRequest.of(0, 10))
                .stream()
                .map(this::toAuditResponse)
                .toList();

        return DashboardResponse.builder()
                .totalUsers(userRepository.count())
                .totalCitizens(userRepository.countByRole(Role.CITIZEN))
                .totalDispatchers(userRepository.countByRole(Role.DISPATCHER))
                .totalResponders(userRepository.countByRole(Role.RESPONDER))
                .activeIncidents(incidentRepository.countByStatusIn(ACTIVE_STATUSES))
                .resolvedIncidents(incidentRepository.countByStatusIn(RESOLVED_STATUSES))
                .pendingResponderApplications(responderRepository.countByApplicationStatus(ResponderApplicationStatus.PENDING))
                .availableResponders(responderRepository.countByAvailabilityAndApplicationStatus(
                        ResponderAvailability.AVAILABLE, ResponderApplicationStatus.APPROVED))
                .recentAuditLogs(recent)
                .build();
    }

    @Transactional(readOnly = true)
    public List<UserResponse> listUsers() {
        return userRepository.findAllByOrderByCreatedAtDesc().stream().map(this::toUserResponse).toList();
    }

    @Transactional
    public UserResponse changeUserStatus(UserPrincipal principal, Long userId, UserStatusChangeRequest request) {
        if (userId.equals(principal.getId())) {
            throw new ForbiddenException("You cannot change your own account status");
        }

        User user = findUserOrThrow(userId);
        boolean previous = user.isActive();
        user.setActive(request.getActive());
        User saved = userRepository.save(user);

        auditLogService.record(principal.getUser(), request.getActive() ? "ACCOUNT_ACTIVATED" : "ACCOUNT_DEACTIVATED",
                "User", userId, "Status changed from " + previous + " to " + request.getActive());

        return toUserResponse(saved);
    }

    /**
     * Section 4/28: role=RESPONDER is refused here — granting that role
     * always goes through approveResponder(), which also creates/updates
     * the Responder profile this endpoint has no way to set up.
     */
    @Transactional
    public UserResponse changeUserRole(UserPrincipal principal, Long userId, UserRoleChangeRequest request) {
        if (userId.equals(principal.getId())) {
            throw new ForbiddenException("You cannot change your own role");
        }
        if (request.getRole() == Role.RESPONDER) {
            throw new ForbiddenException("Use the responder application approval endpoint to grant the RESPONDER role");
        }

        User user = findUserOrThrow(userId);
        Role previous = user.getRole();
        user.setRole(request.getRole());
        User saved = userRepository.save(user);

        auditLogService.record(principal.getUser(), "USER_ROLE_CHANGED", "User", userId,
                previous + " -> " + request.getRole());

        return toUserResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<UserResponse> listDispatchers() {
        return userRepository.findAllByOrderByCreatedAtDesc().stream()
                .filter(u -> u.getRole() == Role.DISPATCHER)
                .map(this::toUserResponse)
                .toList();
    }

    /**
     * Section 7: the only way a dispatcher account comes into existence —
     * never through public registration.
     */
    @Transactional
    public UserResponse createDispatcher(UserPrincipal principal, CreateDispatcherRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException("An account with this email already exists");
        }

        User dispatcher = User.builder()
                .fullName(request.getFullName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .phone(request.getPhone())
                .role(Role.DISPATCHER)
                .active(true)
                .build();
        User saved = userRepository.save(dispatcher);

        auditLogService.record(principal.getUser(), "DISPATCHER_ACCOUNT_CREATED", "User", saved.getId(),
                "Dispatcher account created: " + saved.getEmail());

        return toUserResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<ResponderAdminResponse> listResponders() {
        return responderRepository.findAll().stream().map(this::toResponderResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<ResponderAdminResponse> listResponderApplications() {
        return responderRepository.findByApplicationStatus(ResponderApplicationStatus.PENDING)
                .stream()
                .map(this::toResponderResponse)
                .toList();
    }

    /**
     * Section 6: the only place Role.RESPONDER is ever granted. Sets the
     * responder AVAILABLE immediately on approval — the spec doesn't
     * define a separate on/off-duty toggle for responders, so this is
     * what closes the loop for Section 32's end-to-end demo flow: approve
     * -> immediately assignable by a dispatcher.
     */
    @Transactional
    public ResponderAdminResponse approveResponder(UserPrincipal principal, Long responderId) {
        Responder responder = findResponderOrThrow(responderId);

        if (responder.getApplicationStatus() != ResponderApplicationStatus.PENDING) {
            throw new InvalidStateTransitionException(
                    "Only a PENDING application can be approved (current: " + responder.getApplicationStatus() + ")");
        }

        responder.setApplicationStatus(ResponderApplicationStatus.APPROVED);
        responder.setApprovedAt(LocalDateTime.now());
        responder.setAvailability(ResponderAvailability.AVAILABLE);
        Responder saved = responderRepository.save(responder);

        User user = responder.getUser();
        user.setRole(Role.RESPONDER);
        userRepository.save(user);

        auditLogService.record(principal.getUser(), "RESPONDER_APPROVED", "Responder", responderId,
                "Approved responder application for user " + user.getId());

        return toResponderResponse(saved);
    }

    @Transactional
    public ResponderAdminResponse rejectResponder(UserPrincipal principal, Long responderId) {
        Responder responder = findResponderOrThrow(responderId);

        if (responder.getApplicationStatus() != ResponderApplicationStatus.PENDING) {
            throw new InvalidStateTransitionException(
                    "Only a PENDING application can be rejected (current: " + responder.getApplicationStatus() + ")");
        }

        responder.setApplicationStatus(ResponderApplicationStatus.REJECTED);
        Responder saved = responderRepository.save(responder);

        auditLogService.record(principal.getUser(), "RESPONDER_REJECTED", "Responder", responderId,
                "Rejected responder application for user " + responder.getUser().getId());

        return toResponderResponse(saved);
    }

    @Transactional
    public ResponderAdminResponse suspendResponder(UserPrincipal principal, Long responderId) {
        Responder responder = findResponderOrThrow(responderId);

        if (responder.getApplicationStatus() != ResponderApplicationStatus.APPROVED) {
            throw new InvalidStateTransitionException("Only an approved responder can be suspended");
        }

        responder.setAvailability(ResponderAvailability.SUSPENDED);
        Responder saved = responderRepository.save(responder);

        auditLogService.record(principal.getUser(), "RESPONDER_SUSPENDED", "Responder", responderId,
                "Suspended responder (user " + responder.getUser().getId() + ")");

        return toResponderResponse(saved);
    }

    @Transactional
    public ResponderAdminResponse assignResponderTeam(UserPrincipal principal, Long responderId, AssignTeamRequest request) {
        Responder responder = findResponderOrThrow(responderId);
        ResponderTeam team = responderTeamRepository.findById(request.getTeamId())
                .orElseThrow(() -> new ResourceNotFoundException("Team not found: " + request.getTeamId()));

        responder.setResponderTeam(team);
        Responder saved = responderRepository.save(responder);

        auditLogService.record(principal.getUser(), "RESPONDER_TEAM_ASSIGNED", "Responder", responderId,
                "Assigned to team " + team.getName());

        return toResponderResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<ResponderTeamResponse> listTeams() {
        return responderTeamRepository.findAll().stream()
                .map(t -> new ResponderTeamResponse(t.getId(), t.getName(), t.getDescription()))
                .toList();
    }

    @Transactional
    public ResponderTeamResponse createTeam(ResponderTeamRequest request) {
        if (responderTeamRepository.existsByName(request.getName())) {
            throw new DuplicateResourceException("A team with this name already exists");
        }

        ResponderTeam team = ResponderTeam.builder()
                .name(request.getName())
                .description(request.getDescription())
                .build();
        ResponderTeam saved = responderTeamRepository.save(team);

        return new ResponderTeamResponse(saved.getId(), saved.getName(), saved.getDescription());
    }

    @Transactional(readOnly = true)
    public List<AuditLogResponse> getAuditLogs(int page, int size) {
        return auditLogRepository.findAllByOrderByCreatedAtDesc(PageRequest.of(page, size))
                .stream()
                .map(this::toAuditResponse)
                .toList();
    }

    /**
     * Section 5: "view all incidents" — not narrowed to an active queue
     * like the dispatcher view, since admin needs to see RESOLVED/CLOSED
     * ones too.
     */
    @Transactional(readOnly = true)
    public List<IncidentResponse> listAllIncidents() {
        return incidentRepository.findAllByOrderByCreatedAtDesc().stream().map(IncidentMapper::toResponse).toList();
    }

    private User findUserOrThrow(Long id) {
        return userRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("User not found: " + id));
    }

    private Responder findResponderOrThrow(Long id) {
        return responderRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Responder not found: " + id));
    }

    private UserResponse toUserResponse(User u) {
        return UserResponse.builder()
                .id(u.getId())
                .fullName(u.getFullName())
                .email(u.getEmail())
                .phone(u.getPhone())
                .role(u.getRole())
                .active(u.isActive())
                .createdAt(u.getCreatedAt())
                .build();
    }

    private ResponderAdminResponse toResponderResponse(Responder r) {
        return ResponderAdminResponse.builder()
                .responderId(r.getId())
                .userId(r.getUser().getId())
                .fullName(r.getUser().getFullName())
                .email(r.getUser().getEmail())
                .applicationStatus(r.getApplicationStatus())
                .availability(r.getAvailability())
                .teamId(r.getResponderTeam() != null ? r.getResponderTeam().getId() : null)
                .teamName(r.getResponderTeam() != null ? r.getResponderTeam().getName() : null)
                .appliedAt(r.getAppliedAt())
                .approvedAt(r.getApprovedAt())
                .skills(r.getSkills())
                .build();
    }

    private AuditLogResponse toAuditResponse(AuditLog a) {
        return AuditLogResponse.builder()
                .id(a.getId())
                .actorName(a.getActor() != null ? a.getActor().getFullName() : "System")
                .action(a.getAction())
                .entityType(a.getEntityType())
                .entityId(a.getEntityId())
                .details(a.getDetails())
                .createdAt(a.getCreatedAt())
                .build();
    }
}
