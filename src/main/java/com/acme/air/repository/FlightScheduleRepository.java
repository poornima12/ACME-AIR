package com.acme.air.repository;

import com.acme.air.model.FlightSchedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.ZonedDateTime;
import java.util.List;

@Repository
public interface FlightScheduleRepository extends JpaRepository<FlightSchedule, Long> {

    @Query("SELECT fs FROM FlightSchedule fs " +
            "JOIN fs.flight f " +
            "JOIN f.origin o " +
            "JOIN f.destination d " +
            "WHERE UPPER(o.code) = UPPER(:origin) " +
            "AND UPPER(d.code) = UPPER(:destination) " +
            "AND fs.departureTime >= :startTime " +
            "AND fs.departureTime <= :endTime " +
            "ORDER BY fs.departureTime ASC")
    List<FlightSchedule> findFlightsByRouteAndDateRange(
            @Param("origin") String origin,
            @Param("destination") String destination,
            @Param("startTime") ZonedDateTime startTime,
            @Param("endTime") ZonedDateTime endTime
    );
}
