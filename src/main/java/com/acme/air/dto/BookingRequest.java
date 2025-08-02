package com.acme.air.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;


public record BookingRequest(

        @NotNull(message = "Flight schedule ID is required")
        Long flightScheduleId,
        @NotNull(message = "Passenger details are required")
        @Valid
        List<PassengerDTO> passengers,
        @NotNull(message = "Payment information is required")
        @Valid
        PaymentInfoDTO payment
) {
        public record PassengerDTO(
                @NotBlank(message = "First name is required")
                String firstName,
                @NotBlank(message = "Last name is required")
                String lastName,
                @NotBlank(message = "Email is required")
                @Email(message = "Email must be valid")
                String email,
                @NotBlank(message = "Passport number is required")
                String passportNumber,
                @NotNull(message = "Date of birth is required")
                String selectedSeatNumber
        ) { }

        public record PaymentInfoDTO(
                @NotNull(message = "Payment method is required")
                PaymentMethod method,
                @NotBlank(message = "Transaction ID is required")
                String transactionId,
                @NotNull(message = "Price information is required")
                @Valid
                PriceDTO price,
                @NotNull(message = "Payment status is required")
                PaymentStatus status
        ) {
        }

        public record PriceDTO(
                @NotNull
                @DecimalMin(value = "0.0", inclusive = false, message = "Amount must be positive")
                @Digits(integer = 10, fraction = 2, message = "Amount must have max 2 decimal places")
                BigDecimal amountPaid,
                @NotBlank
                @Pattern(regexp = "^[A-Z]{3}$", message = "Currency must be a 3-letter ISO code (e.g. NZD)")
                String currency
        ) { }

}
