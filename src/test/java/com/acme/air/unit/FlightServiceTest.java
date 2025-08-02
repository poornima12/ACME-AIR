package com.acme.air.unit;


import com.acme.air.dto.FlightSearchResponse;
import com.acme.air.exception.ResourceNotFoundException;
import com.acme.air.model.Airport;
import com.acme.air.model.Flight;
import com.acme.air.model.FlightSchedule;
import com.acme.air.repository.AirportRepository;
import com.acme.air.repository.FlightScheduleRepository;
import com.acme.air.repository.SeatRepository;
import com.acme.air.service.FlightService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FlightServiceTest {

    @Mock
    private FlightScheduleRepository flightScheduleRepository;

    @Mock
    private AirportRepository airportRepository;

    @Mock
    private SeatRepository seatRepository;

    @InjectMocks
    private FlightService flightService;

    private Airport originAirport;
    private Airport destinationAirport;
    private Flight flight;
    private FlightSchedule flightSchedule;
    private LocalDate departureDate;

    @BeforeEach
    void setUp() {
        departureDate = LocalDate.now().plusDays(1);

        originAirport = new Airport();
        originAirport.setCode("JFK");
        originAirport.setName("John F. Kennedy International Airport");
        originAirport.setTimezoneId("America/New_York");

        destinationAirport = new Airport();
        destinationAirport.setCode("LAX");
        destinationAirport.setName("Los Angeles International Airport");
        destinationAirport.setTimezoneId("America/Los_Angeles");

        flight = new Flight();
        flight.setFlightCode("AA123");
        flight.setAirline("American Airlines");
        flight.setOrigin(originAirport);
        flight.setDestination(destinationAirport);

        flightSchedule = new FlightSchedule();
        flightSchedule.setId(1L);
        flightSchedule.setFlight(flight);
        flightSchedule.setDepartureTime(ZonedDateTime.now().plusDays(1).withHour(10));
        flightSchedule.setArrivalTime(ZonedDateTime.now().plusDays(1).withHour(13));
        flightSchedule.setPrice(new BigDecimal("299.99"));
    }

    @Test
    void searchFlights_ValidOneWay_ReturnsFlights() {
        // Arrange
        when(airportRepository.findByCodeIgnoreCase("JFK")).thenReturn(Optional.of(originAirport));
        when(airportRepository.findByCodeIgnoreCase("LAX")).thenReturn(Optional.of(destinationAirport));
        when(flightScheduleRepository.findFlightsByRouteAndDateRange(
                eq("JFK"), eq("LAX"), any(ZonedDateTime.class), any(ZonedDateTime.class)))
                .thenReturn(List.of(flightSchedule));
        when(seatRepository.countAvailableSeatsBySchedule(1L)).thenReturn(5);
        when(seatRepository.findAvailableSeatNumbersBySchedule(1L))
                .thenReturn(Arrays.asList("1A", "1B", "2A", "2B", "3A"));

        // Act
        FlightSearchResponse response = flightService.searchFlights("JFK", "LAX", departureDate, null, 2);

        // Assert
        assertNotNull(response);
        assertEquals(1, response.flights().size());
        FlightSearchResponse.FlightDTO flightDto = response.flights().get(0);
        assertEquals("AA123", flightDto.flightNumber());
        assertEquals("American Airlines", flightDto.airline());
        assertEquals(new BigDecimal("299.99"), flightDto.pricePerSeat());
        assertEquals(new BigDecimal("599.98"), flightDto.totalPrice());
        assertEquals(5, flightDto.availableSeats());
    }

    @Test
    void searchFlights_NoFlightsFound_ThrowsResourceNotFoundException() {
        // Arrange
        when(airportRepository.findByCodeIgnoreCase("JFK")).thenReturn(Optional.of(originAirport));
        when(airportRepository.findByCodeIgnoreCase("LAX")).thenReturn(Optional.of(destinationAirport));
        when(flightScheduleRepository.findFlightsByRouteAndDateRange(
                anyString(), anyString(), any(ZonedDateTime.class), any(ZonedDateTime.class)))
                .thenReturn(Collections.emptyList());

        // Act & Assert
        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () ->
                flightService.searchFlights("JFK", "LAX", departureDate, null, 1));
        assertEquals("No flights found matching the search criteria", exception.getMessage());
    }

    @Test
    void searchFlights_InsufficientSeats_FiltersOutFlight() {
        // Arrange
        when(airportRepository.findByCodeIgnoreCase("JFK")).thenReturn(Optional.of(originAirport));
        when(airportRepository.findByCodeIgnoreCase("LAX")).thenReturn(Optional.of(destinationAirport));
        when(flightScheduleRepository.findFlightsByRouteAndDateRange(
                anyString(), anyString(), any(ZonedDateTime.class), any(ZonedDateTime.class)))
                .thenReturn(List.of(flightSchedule));
        when(seatRepository.countAvailableSeatsBySchedule(1L)).thenReturn(1); // Only 1 seat available

        // Act & Assert
        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () ->
                flightService.searchFlights("JFK", "LAX", departureDate, null, 2)); // Need 2 seats
        assertEquals("No flights found matching the search criteria", exception.getMessage());
    }

    @Test
    void searchFlights_NullOrigin_ThrowsIllegalArgumentException() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                flightService.searchFlights(null, "LAX", departureDate, null, 1));
        assertEquals("Origin airport code is required", exception.getMessage());
    }

    @Test
    void searchFlights_EmptyOrigin_ThrowsIllegalArgumentException() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                flightService.searchFlights("  ", "LAX", departureDate, null, 1));
        assertEquals("Origin airport code is required", exception.getMessage());
    }

    @Test
    void searchFlights_NullDestination_ThrowsIllegalArgumentException() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                flightService.searchFlights("JFK", null, departureDate, null, 1));
        assertEquals("Destination airport code is required", exception.getMessage());
    }

    @Test
    void searchFlights_SameOriginDestination_ThrowsIllegalArgumentException() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                flightService.searchFlights("JFK", "jfk", departureDate, null, 1));
        assertEquals("Origin and destination cannot be the same", exception.getMessage());
    }

    @Test
    void searchFlights_NullDepartureDate_ThrowsIllegalArgumentException() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                flightService.searchFlights("JFK", "LAX", null, null, 1));
        assertEquals("Departure date is required", exception.getMessage());
    }

    @Test
    void searchFlights_PastDepartureDate_ThrowsIllegalArgumentException() {
        LocalDate pastDate = LocalDate.now().minusDays(1);
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                flightService.searchFlights("JFK", "LAX", pastDate, null, 1));
        assertEquals("Departure date cannot be in the past", exception.getMessage());
    }

    @Test
    void searchFlights_ZeroPassengers_ThrowsIllegalArgumentException() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                flightService.searchFlights("JFK", "LAX", departureDate, null, 0));
        assertEquals("Number of passengers must be greater than 0", exception.getMessage());
    }

    @Test
    void searchFlights_NegativePassengers_ThrowsIllegalArgumentException() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                flightService.searchFlights("JFK", "LAX", departureDate, null, -1));
        assertEquals("Number of passengers must be greater than 0", exception.getMessage());
    }

    @Test
    void searchFlights_TooManyPassengers_ThrowsIllegalArgumentException() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                flightService.searchFlights("JFK", "LAX", departureDate, null, 10));
        assertEquals("Maximum 9 passengers allowed per booking", exception.getMessage());
    }

    @Test
    void searchFlights_InvalidOriginIATACode_ThrowsIllegalArgumentException() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                flightService.searchFlights("JFKX", "LAX", departureDate, null, 1));
        assertEquals("Invalid origin airport code format", exception.getMessage());
    }

    @Test
    void searchFlights_InvalidDestinationIATACode_ThrowsIllegalArgumentException() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                flightService.searchFlights("JFK", "LA", departureDate, null, 1));
        assertEquals("Invalid destination airport code format", exception.getMessage());
    }

    @Test
    void searchFlights_LowercaseIATACodes_ThrowsIllegalArgumentException() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                flightService.searchFlights("jfk", "LAX", departureDate, null, 1));
        assertEquals("Invalid origin airport code format", exception.getMessage());
    }

    @Test
    void searchFlights_NonExistentOriginAirport_ThrowsResourceNotFoundException() {
        // Arrange
        when(airportRepository.findByCodeIgnoreCase("JFK")).thenReturn(Optional.empty());

        // Act & Assert
        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () ->
                flightService.searchFlights("JFK", "LAX", departureDate, null, 1));
        assertEquals("Origin airport not found: JFK", exception.getMessage());
    }

    @Test
    void searchFlights_NonExistentDestinationAirport_ThrowsResourceNotFoundException() {
        // Arrange
        when(airportRepository.findByCodeIgnoreCase("JFK")).thenReturn(Optional.of(originAirport));
        when(airportRepository.findByCodeIgnoreCase("LAX")).thenReturn(Optional.empty());

        // Act & Assert
        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () ->
                flightService.searchFlights("JFK", "LAX", departureDate, null, 1));
        assertEquals("Destination airport not found: LAX", exception.getMessage());
    }

    @Test
    void searchFlights_MaximumPassengersAllowed_Success() {
        // Arrange
        when(airportRepository.findByCodeIgnoreCase("JFK")).thenReturn(Optional.of(originAirport));
        when(airportRepository.findByCodeIgnoreCase("LAX")).thenReturn(Optional.of(destinationAirport));
        when(flightScheduleRepository.findFlightsByRouteAndDateRange(
                anyString(), anyString(), any(ZonedDateTime.class), any(ZonedDateTime.class)))
                .thenReturn(List.of(flightSchedule));
        when(seatRepository.countAvailableSeatsBySchedule(1L)).thenReturn(10);
        when(seatRepository.findAvailableSeatNumbersBySchedule(1L))
                .thenReturn(Arrays.asList("1A", "1B", "2A", "2B", "3A", "3B", "4A", "4B", "5A", "5B"));

        // Act
        FlightSearchResponse response = flightService.searchFlights("JFK", "LAX", departureDate, null, 9);

        // Assert
        assertNotNull(response);
        assertEquals(1, response.flights().size());
        FlightSearchResponse.FlightDTO flightDto = response.flights().get(0);
        assertEquals(new BigDecimal("2699.91"), flightDto.totalPrice()); // 299.99 * 9
    }
}

