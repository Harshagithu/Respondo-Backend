package com.respondo.repository;

import com.respondo.entity.ResponderTeam;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ResponderTeamRepository extends JpaRepository<ResponderTeam, Long> {

    boolean existsByName(String name);
}
