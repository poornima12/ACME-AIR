package com.acme.air.repository;

import com.acme.air.model.SeatLock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface SeatLockRepository extends JpaRepository<SeatLock, Long> {
    @Query("SELECT sl FROM SeatLock sl WHERE sl.seat.id = :seatId AND sl.status = 'ACTIVE' AND sl.expiresAt > :now")
    Optional<SeatLock> findActiveLockBySeatId(@Param("seatId") Long seatId, @Param("now") LocalDateTime now);

    @Query("SELECT sl FROM SeatLock sl WHERE sl.sessionId = :sessionId AND sl.status = 'ACTIVE'")
    List<SeatLock> findActiveLocksForSession(@Param("sessionId") String sessionId);

    @Query("SELECT sl FROM SeatLock sl WHERE sl.sessionId = :sessionId AND sl.seat.id = :seatId")
    Optional<SeatLock> findBySessionIdAndSeatId(@Param("sessionId") String sessionId, @Param("seatId") Long seatId);

    @Query("SELECT sl FROM SeatLock sl WHERE sl.seat.id = :seatId AND sl.expiresAt <= :now AND sl.status = 'ACTIVE'")
    List<SeatLock> findExpiredLocksBySeatId(@Param("seatId") Long seatId, @Param("now") LocalDateTime now);

    @Modifying
    @Query("UPDATE SeatLock sl SET sl.status = 'RELEASED' WHERE sl.seat.id IN :seatIds AND sl.status = 'ACTIVE'")
    void releaseLocksForSeats(@Param("seatIds") List<Long> seatIds);
}
