package com.respondo.repository;

import com.respondo.entity.Responder;
import com.respondo.enums.ResponderApplicationStatus;
import com.respondo.enums.ResponderAvailability;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ResponderRepository extends JpaRepository<Responder, Long> {

    Optional<Responder> findByUserId(Long userId);

    List<Responder> findByApplicationStatus(ResponderApplicationStatus status);

    List<Responder> findByAvailability(ResponderAvailability availability);

    long countByApplicationStatus(ResponderApplicationStatus status);

    long countByAvailabilityAndApplicationStatus(ResponderAvailability availability, ResponderApplicationStatus applicationStatus);
}
