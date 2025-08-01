package com.acme.air.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

public record BookingRequest(
        @NotBlank(message = "Flight number is required")
        String flightNumber,
        @NotNull(message = "Departure date is required")
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        @JsonFormat(pattern = "yyyy-MM-dd")
        LocalDate departureDate,
        @Pattern(regexp = "WINDOW|AISLE|MIDDLE", message = "Seat preference must be WINDOW, AISLE, or MIDDLE")
        String seatPreference,
        @NotNull(message = "Passenger details are required")
        @Valid
        PassengerDTO passenger,
        @NotNull(message = "Payment information is required")
        @Valid
        PaymentInfoDTO paymentInfo
) { }
