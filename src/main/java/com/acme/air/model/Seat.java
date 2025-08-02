package com.acme.air.model;

import jakarta.persistence.*;

@Entity
public class Seat extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String seatNumber;

    @ManyToOne
    private FlightSchedule schedule;

    @Enumerated(EnumType.STRING)
    private SeatStatus status = SeatStatus.AVAILABLE;

    public enum SeatStatus {
        AVAILABLE, LOCKED, BOOKED
    }
}

