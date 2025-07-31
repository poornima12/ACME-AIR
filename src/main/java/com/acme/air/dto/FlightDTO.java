package com.acme.air.dto;

import java.time.ZonedDateTime;

public record FlightDTO(
        String flightNumber,
        String airline,
        String origin,
        String destination,
        ZonedDateTime departureTime,
        ZonedDateTime arrivalTime,
        String duration,
        int stops,
        PriceDTO price,
        String cabinClass,
        int availableSeats
) {
}
