package com.respondo.repository;

import com.respondo.entity.Incident;
import com.respondo.enums.IncidentStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface IncidentRepository extends JpaRepository<Incident, Long> {

    List<Incident> findByCitizenIdOrderByCreatedAtDesc(Long citizenId);

    List<Incident> findByStatusOrderByCreatedAtAsc(IncidentStatus status);

    List<Incident> findByStatusInOrderByCreatedAtAsc(List<IncidentStatus> statuses);

    long countByStatusIn(List<IncidentStatus> statuses);

    List<Incident> findAllByOrderByCreatedAtDesc();
}
