package com.respondo.service;

import com.respondo.entity.AuditLog;
import com.respondo.entity.User;
import com.respondo.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Section 16: one place that writes AuditLog rows, called from every
 * module that produces an auditable action (auth, incident, dispatcher,
 * responder now; admin in Phase 7). actor is nullable so a genuinely
 * system-triggered entry doesn't need a fabricated user to attach to.
 */
@Service
@RequiredArgsConstructor
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;

    @Transactional
    public void record(User actor, String action, String entityType, Long entityId, String details) {
        AuditLog log = AuditLog.builder()
                .actor(actor)
                .action(action)
                .entityType(entityType)
                .entityId(entityId)
                .details(details)
                .build();
        auditLogRepository.save(log);
    }

    @Transactional
    public void recordSystemAction(String action, String entityType, Long entityId, String details) {
        record(null, action, entityType, entityId, details);
    }
}
