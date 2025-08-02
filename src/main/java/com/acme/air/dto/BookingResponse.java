package com.acme.air.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.List;

public record BookingResponse(
        String bookingId,
        String status,
        String flightNumber,
        ZonedDateTime departureDate,
        List<PassengerSeatDTO> passengers,
        PaymentInfoDTO payment,
        LocalDate createdAt
) {
    public record PassengerSeatDTO(
            String firstName,
            String lastName,
            String email,
            String seatNumber
    ) {}

    public record PaymentInfoDTO(
            String transactionId,
            BigDecimal amountPaid,
            String currency
    ) {}
}
