package com.acme.air.dto;

import java.time.LocalDate;

public record BookingRequest(
        String flightNumber,
        LocalDate departureDate,
        String seatPreference, // Optional: WINDOW, AISLE, MIDDLE
        PassengerDTO passenger,
        PaymentInfoDTO paymentInfo
) { }
