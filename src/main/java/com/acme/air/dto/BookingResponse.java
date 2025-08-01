package com.acme.air.dto;

import java.time.LocalDate;
import java.time.ZonedDateTime;

public record BookingResponse(
        String bookingId,
        String status,
        String flightNumber,
        LocalDate departureDate,
        PassengerDTO passenger,
        String seat,
        PriceDTO price,
        ZonedDateTime createdAt
) { }
