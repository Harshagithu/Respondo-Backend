package com.respondo.repository;

import com.respondo.entity.IncidentCategory;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IncidentCategoryRepository extends JpaRepository<IncidentCategory, Long> {

    boolean existsByName(String name);
}
