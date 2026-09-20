package com.smartrebook.booking.repository;

import com.smartrebook.booking.domain.Incident;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IncidentRepository extends JpaRepository<Incident, Long> {
}
