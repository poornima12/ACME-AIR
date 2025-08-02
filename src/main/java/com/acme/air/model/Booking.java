package com.acme.air.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.time.ZonedDateTime;

@EqualsAndHashCode(callSuper = true)
@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
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

