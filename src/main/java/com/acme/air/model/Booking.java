package com.acme.air.model;

import jakarta.persistence.*;

import java.time.LocalDateTime;

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

    private LocalDateTime bookingTime;
    private BookingStatus status;

    public enum BookingStatus {
        CONFIRMED, CANCELLED
    }
}

