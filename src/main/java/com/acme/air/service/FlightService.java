package com.acme.air.service;

import com.acme.air.dto.FlightSearchResponse;
import com.acme.air.exception.ResourceNotFoundException;
import com.acme.air.model.Airport;
import com.acme.air.model.FlightSchedule;
import com.acme.air.repository.AirportRepository;
import com.acme.air.repository.FlightScheduleRepository;
import com.acme.air.repository.SeatRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class FlightService {

    private static final Logger logger = LoggerFactory.getLogger(FlightService.class);

    @Autowired
    private FlightScheduleRepository flightScheduleRepository;

    @Autowired
    private AirportRepository airportRepository;

    @Autowired
    private SeatRepository seatRepository;

    public FlightSearchResponse searchFlights(String origin, String destination,
                                              LocalDate departureDate, LocalDate returnDate,
                                              int numberOfPassengers) {

        logger.info("Searching flights: {} -> {}, departure: {}, passengers: {}",
                origin, destination, departureDate, numberOfPassengers);

        // Validate input parameters
        validateSearchCriteria(origin, destination, departureDate, numberOfPassengers);

        // Validate airport codes exist
        validateAirportCodes(origin, destination);

        // Search for outbound flights
        List<FlightSearchResponse.FlightDTO> outboundFlights = searchOneWayFlights(origin, destination, departureDate, numberOfPassengers);

        // For round-trip, search return flights
        List<FlightSearchResponse.FlightDTO> returnFlights = null;
        if (returnDate != null) {
            logger.info("Searching return flights: {} -> {}, return: {}", destination, origin, returnDate);
            returnFlights = searchOneWayFlights(destination, origin, returnDate, numberOfPassengers);
        }

        // Combine results - for now just return outbound flights
        // In a real system, you might want to create flight combinations
        List<FlightSearchResponse.FlightDTO> allFlights = outboundFlights;

        logger.info("Found {} flights for search criteria", allFlights.size());

        if (allFlights.isEmpty()) {
            throw new ResourceNotFoundException("No flights found matching the search criteria");
        }

        return new FlightSearchResponse(allFlights);
    }

    private void validateSearchCriteria(String origin, String destination, LocalDate departureDate, int numberOfPassengers) {
        if (origin == null || origin.trim().isEmpty()) {
            throw new IllegalArgumentException("Origin airport code is required");
        }

        if (destination == null || destination.trim().isEmpty()) {
            throw new IllegalArgumentException("Destination airport code is required");
        }

        if (origin.equalsIgnoreCase(destination)) {
            throw new IllegalArgumentException("Origin and destination cannot be the same");
        }

        if (departureDate == null) {
            throw new IllegalArgumentException("Departure date is required");
        }

        if (departureDate.isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("Departure date cannot be in the past");
        }

        if (numberOfPassengers <= 0) {
            throw new IllegalArgumentException("Number of passengers must be greater than 0");
        }

        if (numberOfPassengers > 9) {
            throw new IllegalArgumentException("Maximum 9 passengers allowed per booking");
        }

        // Validate airport code format (IATA codes are 3 letters)
        if (!isValidIATACode(origin)) {
            throw new IllegalArgumentException("Invalid origin airport code format");
        }

        if (!isValidIATACode(destination)) {
            throw new IllegalArgumentException("Invalid destination airport code format");
        }
    }

    private boolean isValidIATACode(String code) {
        return code != null && code.matches("^[A-Z]{3}$");
    }

    private void validateAirportCodes(String origin, String destination) {
        Airport originAirport = airportRepository.findByCodeIgnoreCase(origin)
                .orElseThrow(() -> new ResourceNotFoundException("Origin airport not found: " + origin));

        Airport destinationAirport = airportRepository.findByCodeIgnoreCase(destination)
                .orElseThrow(() -> new ResourceNotFoundException("Destination airport not found: " + destination));

        logger.debug("Validated airports: {} ({}) -> {} ({})",
                origin, originAirport.getName(), destination, destinationAirport.getName());
    }

    private List<FlightSearchResponse.FlightDTO> searchOneWayFlights(String origin, String destination,
                                                                     LocalDate departureDate, int numberOfPassengers) {

        // Get start and end of the departure date in the system timezone
        ZonedDateTime startOfDay = departureDate.atStartOfDay(ZonedDateTime.now().getZone());
        ZonedDateTime endOfDay = startOfDay.plusDays(1).minusNanos(1);

        logger.debug("Searching flights between {} and {}", startOfDay, endOfDay);

        // Find flight schedules for the route and date
        List<FlightSchedule> schedules = flightScheduleRepository
                .findFlightsByRouteAndDateRange(origin.toUpperCase(), destination.toUpperCase(),
                        startOfDay, endOfDay);

        logger.debug("Found {} flight schedules for route", schedules.size());

        // Filter schedules that have enough available seats and convert to DTOs
        return schedules.stream()
                .filter(schedule -> hasEnoughAvailableSeats(schedule, numberOfPassengers))
                .map(schedule -> mapToFlightDTO(schedule, numberOfPassengers))
                .collect(Collectors.toList());
    }

    private boolean hasEnoughAvailableSeats(FlightSchedule schedule, int numberOfPassengers) {
        int availableSeats = seatRepository.countAvailableSeatsBySchedule(schedule.getId());
        boolean hasEnough = availableSeats >= numberOfPassengers;

        if (!hasEnough) {
            logger.debug("Flight {} has only {} available seats, need {}",
                    schedule.getFlight().getFlightCode(), availableSeats, numberOfPassengers);
        }

        return hasEnough;
    }

    private FlightSearchResponse.FlightDTO mapToFlightDTO(FlightSchedule schedule, int numberOfPassengers) {
        // Get available seat numbers
        List<String> availableSeatNumbers = seatRepository.findAvailableSeatNumbersBySchedule(schedule.getId());
        int availableSeats = availableSeatNumbers.size();

        // Calculate pricing
        BigDecimal pricePerSeat = schedule.getPrice();
        BigDecimal totalPrice = pricePerSeat.multiply(BigDecimal.valueOf(numberOfPassengers));

        Airport origin = schedule.getFlight().getOrigin();
        Airport destination = schedule.getFlight().getDestination();

        ZoneId originZone = ZoneId.of(origin.getTimezoneId());
        ZoneId destinationZone = ZoneId.of(destination.getTimezoneId());

        return new FlightSearchResponse.FlightDTO(
                schedule.getId(),
                schedule.getFlight().getFlightCode(),
                schedule.getFlight().getAirline(),
                schedule.getFlight().getOrigin().getCode(),
                schedule.getFlight().getDestination().getCode(),
                schedule.getDepartureTime().withZoneSameInstant(originZone),
                schedule.getArrivalTime().withZoneSameInstant(destinationZone),
                pricePerSeat,
                numberOfPassengers,
                totalPrice,
                availableSeats,
                availableSeatNumbers
        );
    }
}
