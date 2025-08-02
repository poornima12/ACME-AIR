package com.acme.air.repository;

import com.acme.air.model.FlightSchedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FlightScheduleRepository extends JpaRepository<FlightSchedule, Long> {
}
