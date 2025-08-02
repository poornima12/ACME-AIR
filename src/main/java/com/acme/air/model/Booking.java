package com.acme.air.model;

import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.time.ZonedDateTime;

@Entity
public class Booking extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    private Passenger passenger;

    @ManyToOne
    private FlightSchedule schedule;

    @OneToOne
    private Seat seat;

    private ZonedDateTime bookingTime;
    private BookingStatus status;

    public enum BookingStatus {
        CONFIRMED,
        CANCELLED,
        EXPIRED,
        REFUNDED
    }
}

