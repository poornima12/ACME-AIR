package com.acme.air.repository;

import com.acme.air.model.Seat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SeatRepository extends JpaRepository<Seat, Long> {
    @Query("SELECT COUNT(s) FROM Seat s WHERE s.schedule.id = :scheduleId AND s.status = 'AVAILABLE'")
    int countAvailableSeatsBySchedule(@Param("scheduleId") Long scheduleId);

    @Query("SELECT s.seatNumber FROM Seat s WHERE s.schedule.id = :scheduleId AND s.status = 'AVAILABLE' ORDER BY s.seatNumber")
    List<String> findAvailableSeatNumbersBySchedule(@Param("scheduleId") Long scheduleId);
}
