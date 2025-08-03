package com.acme.air.repository;

import com.acme.air.model.Booking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {

    @Query("SELECT b FROM Booking b JOIN b.bookingItems bi WHERE bi.passenger.id = :passengerId AND b.schedule.id = :scheduleId AND b.status = :status")
    Optional<Booking> findByPassengerAndSchedule(@Param("passengerId") Long passengerId,
                                                 @Param("scheduleId") Long scheduleId,
                                                 @Param("status") Booking.BookingStatus status);

    @Query("SELECT COUNT(bi) FROM BookingItem bi WHERE bi.booking.schedule.id = :scheduleId AND bi.booking.status = :status")
    long countConfirmedBookingsBySchedule(@Param("scheduleId") Long scheduleId, @Param("status") Booking.BookingStatus status);

    Optional<Booking> findByBookingReference(String bookingReference);

}
