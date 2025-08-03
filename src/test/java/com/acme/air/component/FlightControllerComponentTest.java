package com.acme.air.component;


import com.acme.air.controller.FlightController;
import com.acme.air.model.Airport;
import com.acme.air.model.Flight;
import com.acme.air.model.FlightSchedule;
import com.acme.air.repository.AirportRepository;
import com.acme.air.repository.FlightScheduleRepository;
import com.acme.air.repository.SeatRepository;
import com.acme.air.service.FlightService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;


@WebMvcTest(FlightController.class)
@Import(FlightService.class)
class FlightControllerComponentTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private FlightScheduleRepository flightScheduleRepository;

    @MockBean
    private AirportRepository airportRepository;

    @MockBean
    private SeatRepository seatRepository;

    @Test
    @DisplayName("SUCCESS: Should return available flights for valid search criteria")
    void shouldReturnAvailableFlights_WhenValidSearchCriteria() throws Exception {
        // Given
        String origin = "LAX";
        String destination = "JFK";
        LocalDate departureDate = LocalDate.now().plusDays(7);
        int passengers = 2;

        // Mock airports
        Airport laxAirport = createAirport("LAX", "Los Angeles International", "America/Los_Angeles");
        Airport jfkAirport = createAirport("JFK", "John F. Kennedy International", "America/New_York");

        when(airportRepository.findByCodeIgnoreCase("LAX")).thenReturn(Optional.of(laxAirport));
        when(airportRepository.findByCodeIgnoreCase("JFK")).thenReturn(Optional.of(jfkAirport));

        // Mock flight schedules
        FlightSchedule schedule1 = createFlightSchedule(1L, "AA123", "American Airlines",
                laxAirport, jfkAirport, departureDate, new BigDecimal("299.99"));
        FlightSchedule schedule2 = createFlightSchedule(2L, "UA456", "United Airlines",
                laxAirport, jfkAirport, departureDate, new BigDecimal("319.99"));

        List<FlightSchedule> schedules = Arrays.asList(schedule1, schedule2);

        when(flightScheduleRepository.findFlightsByRouteAndDateRange(
                eq("LAX"), eq("JFK"), any(ZonedDateTime.class), any(ZonedDateTime.class)))
                .thenReturn(schedules);

        // Mock seat availability
        when(seatRepository.countAvailableSeatsBySchedule(1L)).thenReturn(50);
        when(seatRepository.countAvailableSeatsBySchedule(2L)).thenReturn(30);

        when(seatRepository.findAvailableSeatNumbersBySchedule(1L))
                .thenReturn(Arrays.asList("1A", "1B", "2A", "2B", "3A"));
        when(seatRepository.findAvailableSeatNumbersBySchedule(2L))
                .thenReturn(Arrays.asList("10A", "10B", "11A", "11B"));

        // When & Then
        mockMvc.perform(get("/api/v1/flights/search")
                        .param("origin", origin)
                        .param("destination", destination)
                        .param("departureDate", departureDate.toString())
                        .param("passengers", String.valueOf(passengers)))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/json"))
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.data.flights").isArray())
                .andExpect(jsonPath("$.data.flights.length()").value(2))
                .andExpect(jsonPath("$.data.flights[0].flightNumber").value("AA123"))
                .andExpect(jsonPath("$.data.flights[0].airline").value("American Airlines"))
                .andExpect(jsonPath("$.data.flights[0].origin").value("LAX"))
                .andExpect(jsonPath("$.data.flights[0].destination").value("JFK"))
                .andExpect(jsonPath("$.data.flights[0].pricePerSeat").value(299.99))
                .andExpect(jsonPath("$.data.flights[0].numberOfPassengers").value(2))
                .andExpect(jsonPath("$.data.flights[0].totalPrice").value(599.98))
                .andExpect(jsonPath("$.data.flights[0].availableSeats").value(5))
                .andExpect(jsonPath("$.data.flights[1].flightNumber").value("UA456"))
                .andExpect(jsonPath("$.data.flights[1].airline").value("United Airlines"))
                .andExpect(jsonPath("$.data.flights[1].pricePerSeat").value(319.99))
                .andExpect(jsonPath("$.data.flights[1].totalPrice").value(639.98))
                .andExpect(jsonPath("$.data.flights[1].availableSeats").value(4));
    }

    @Test
    @DisplayName("FAILURE: Should return 404 when no flights found")
    void shouldReturn404_WhenNoFlightsFound() throws Exception {
        // Given
        String origin = "LAX";
        String destination = "JFK";
        LocalDate departureDate = LocalDate.now().plusDays(7);
        int passengers = 2;

        // Mock airports (valid airports exist)
        Airport laxAirport = createAirport("LAX", "Los Angeles International", "America/Los_Angeles");
        Airport jfkAirport = createAirport("JFK", "John F. Kennedy International", "America/New_York");

        when(airportRepository.findByCodeIgnoreCase("LAX")).thenReturn(Optional.of(laxAirport));
        when(airportRepository.findByCodeIgnoreCase("JFK")).thenReturn(Optional.of(jfkAirport));

        // Mock no flight schedules found
        when(flightScheduleRepository.findFlightsByRouteAndDateRange(
                eq("LAX"), eq("JFK"), any(ZonedDateTime.class), any(ZonedDateTime.class)))
                .thenReturn(List.of()); // Empty list - no flights found

        // When & Then
        mockMvc.perform(get("/api/v1/flights/search")
                        .param("origin", origin)
                        .param("destination", destination)
                        .param("departureDate", departureDate.toString())
                        .param("passengers", String.valueOf(passengers)))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType("application/json"))
                .andExpect(jsonPath("$.error").exists())
                .andExpect(jsonPath("$.error.message").value("No flights found matching the search criteria"));
    }

    // Helper methods to create test data
    private Airport createAirport(String code, String name, String timezoneId) {
        Airport airport = new Airport();
        airport.setCode(code);
        airport.setName(name);
        airport.setTimezoneId(timezoneId);
        return airport;
    }

    private FlightSchedule createFlightSchedule(Long id, String flightCode, String airline,
                                                Airport origin, Airport destination, LocalDate departureDate, BigDecimal price) {

        Flight flight = new Flight();
        flight.setFlightCode(flightCode);
        flight.setAirline(airline);
        flight.setOrigin(origin);
        flight.setDestination(destination);

        FlightSchedule schedule = new FlightSchedule();
        schedule.setId(id);
        schedule.setFlight(flight);
        schedule.setPrice(price);

        // Set departure and arrival times (example: 6-hour flight)
        ZonedDateTime departure = departureDate.atTime(10, 0)
                .atZone(ZoneId.of(origin.getTimezoneId()));
        ZonedDateTime arrival = departure.plusHours(6)
                .withZoneSameInstant(ZoneId.of(destination.getTimezoneId()));

        schedule.setDepartureTime(departure);
        schedule.setArrivalTime(arrival);

        return schedule;
    }
}