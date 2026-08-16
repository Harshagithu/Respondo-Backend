package com.respondo.repository;

import com.respondo.entity.Assignment;
import com.respondo.enums.AssignmentStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AssignmentRepository extends JpaRepository<Assignment, Long> {

    List<Assignment> findByResponderIdOrderByAssignedAtDesc(Long responderId);

    Optional<Assignment> findByIncidentIdAndStatus(Long incidentId, AssignmentStatus status);

    List<Assignment> findByIncidentIdOrderByAssignedAtDesc(Long incidentId);
}
