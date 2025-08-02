package com.acme.air.model;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.ZonedDateTime;

@Entity
public class FlightSchedule extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    private Flight flight;

    private ZonedDateTime departureTime;
    private ZonedDateTime arrivalTime;
    private BigDecimal price;
    private String currency = "NZD";

    private Integer totalSeats;
}

