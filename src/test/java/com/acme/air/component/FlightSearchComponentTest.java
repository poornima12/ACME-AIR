package com.acme.air.component;

import com.acme.air.controller.FlightController;
import com.acme.air.dto.FlightSearchResponse;
import com.acme.air.exception.ResourceNotFoundException;
import com.acme.air.model.Airport;
import com.acme.air.model.Flight;
import com.acme.air.model.FlightSchedule;
import com.acme.air.repository.AirportRepository;
import com.acme.air.repository.FlightScheduleRepository;
import com.acme.air.repository.SeatRepository;
import com.acme.air.service.FlightService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.mockito.Mockito.when;

@WebMvcTest(FlightController.class)
@DisplayName("Flight Search Component Tests")
class FlightSearchComponentTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private FlightService flightService;

    @MockitoBean
    private FlightScheduleRepository flightScheduleRepository;

    @MockitoBean
    private AirportRepository airportRepository;

    @MockitoBean
    private SeatRepository seatRepository;

    @Autowired
    private ObjectMapper objectMapper;

    private Airport aucklandAirport;
    private Airport wellingtonAirport;
    private FlightSchedule mockSchedule;
    private Flight mockFlight;

    @BeforeEach
    void setUp() {
        aucklandAirport = new Airport();
        aucklandAirport.setId(1L);
        aucklandAirport.setCode("AKL");
        aucklandAirport.setName("Auckland Airport");
        aucklandAirport.setTimezoneId("Pacific/Auckland");

        wellingtonAirport = new Airport();
        wellingtonAirport.setId(2L);
        wellingtonAirport.setCode("WLG");
        wellingtonAirport.setName("Wellington Airport");
        wellingtonAirport.setTimezoneId("Pacific/Auckland");

        mockFlight = new Flight();
        mockFlight.setId(1L);
        mockFlight.setFlightCode("NZ123");
        mockFlight.setAirline("Air New Zealand");
        mockFlight.setOrigin(aucklandAirport);
        mockFlight.setDestination(wellingtonAirport);

        mockSchedule = new FlightSchedule();
        mockSchedule.setId(1L);
        mockSchedule.setFlight(mockFlight);
        mockSchedule.setDepartureTime(ZonedDateTime.now().plusDays(1));
        mockSchedule.setArrivalTime(ZonedDateTime.now().plusDays(1).plusHours(2));
        mockSchedule.setPrice(BigDecimal.valueOf(299.00));
        mockSchedule.setTotalSeats(180);
    }

    @Test
    @DisplayName("SUCCESS: Should return available flights for valid search criteria")
    void searchFlights_ValidCriteria_ReturnsAvailableFlights() throws Exception {
        // Given
        LocalDate departureDate = LocalDate.now().plusDays(1);
        List<String> availableSeats = Arrays.asList("1A", "1B", "2A", "2B", "3A");

        FlightSearchResponse.FlightDTO flightDTO = new FlightSearchResponse.FlightDTO(
                1L,
                "NZ123",
                "Air New Zealand",
                "AKL",
                "WLG",
                ZonedDateTime.now().plusDays(1),
                ZonedDateTime.now().plusDays(1).plusHours(2),
                BigDecimal.valueOf(299.00),
                2,
                BigDecimal.valueOf(598.00),
                150,
                availableSeats
        );

        FlightSearchResponse mockResponse = new FlightSearchResponse(Collections.singletonList(flightDTO));

        when(flightService.searchFlights("AKL", "WLG", departureDate, null, 2))
                .thenReturn(mockResponse);

        // When & Then
        mockMvc.perform(get("/api/v1/flights/search")
                        .param("origin", "AKL")
                        .param("destination", "WLG")
                        .param("departureDate", departureDate.toString())
                        .param("passengers", "2")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status", is("SUCCESS")))
                .andExpect(jsonPath("$.data.flights", hasSize(1)))
                .andExpect(jsonPath("$.data.flights[0].flightScheduleId", is(1)))
                .andExpect(jsonPath("$.data.flights[0].flightNumber", is("NZ123")))
                .andExpect(jsonPath("$.data.flights[0].airline", is("Air New Zealand")))
                .andExpect(jsonPath("$.data.flights[0].origin", is("AKL")))
                .andExpect(jsonPath("$.data.flights[0].destination", is("WLG")))
                .andExpect(jsonPath("$.data.flights[0].pricePerSeat", is(299.00)))
                .andExpect(jsonPath("$.data.flights[0].numberOfPassengers", is(2)))
                .andExpect(jsonPath("$.data.flights[0].totalPrice", is(598.00)))
                .andExpect(jsonPath("$.data.flights[0].availableSeats", is(150)))
                .andExpect(jsonPath("$.data.flights[0].availableSeatNumbers", hasSize(5)))
                .andExpect(jsonPath("$.data.flights[0].availableSeatNumbers", containsInAnyOrder("1A", "1B", "2A", "2B", "3A")));

        verify(flightService).searchFlights("AKL", "WLG", departureDate, null, 2);
    }

    @Test
    @DisplayName("FAILURE: Should return 404 when no flights found")
    void searchFlights_NoFlightsFound_Returns404() throws Exception {
        // Given
        LocalDate departureDate = LocalDate.now().plusDays(1);

        when(flightService.searchFlights("AKL", "WLG", departureDate, null, 2))
                .thenThrow(new ResourceNotFoundException("No flights found matching the search criteria"));

        // When & Then
        mockMvc.perform(get("/api/v1/flights/search")
                        .param("origin", "AKL")
                        .param("destination", "WLG")
                        .param("departureDate", departureDate.toString())
                        .param("passengers", "2")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }
}
