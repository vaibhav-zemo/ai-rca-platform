package org.vaibhav.aircaplatform.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.vaibhav.aircaplatform.model.Incident;

import java.util.Optional;

public interface IncidentRepository extends JpaRepository<Incident, String> {
    Optional<Incident> findByTraceId(String traceId);
}
