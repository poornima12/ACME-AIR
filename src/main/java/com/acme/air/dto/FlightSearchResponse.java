package com.acme.air.dto;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.List;

public record FlightSearchResponse(
        List<FlightDTO> flights
) {
    public record FlightDTO(
            Long flightScheduleId,
            String flightNumber,
            String airline,
            String origin,
            String destination,
            ZonedDateTime departureTime,
            ZonedDateTime arrivalTime,
            BigDecimal pricePerSeat,
            int numberOfPassengers,
            BigDecimal totalPrice,
            int availableSeats,
            List<String> availableSeatNumbers
    ) {
    }

}
