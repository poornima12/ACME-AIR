package com.acme.air.controller;

import com.acme.air.api.FlightsApi;
import com.acme.air.dto.ApiResponse;
import com.acme.air.dto.FlightSearchResponse;
import com.acme.air.generated.dto.FlightsResponseWrapper;
import com.acme.air.service.FlightService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.ArrayList;

import static com.acme.air.mapper.DTOMapper.convertToGeneratedDTO;

@RestController
@RequestMapping("/api/v1/flights")
public class FlightController implements FlightsApi {

    private static final Logger logger = LoggerFactory.getLogger(FlightController.class);

    @Autowired
    private FlightService flightService;

    @Override
    @GetMapping("/search")
    public ResponseEntity<FlightsResponseWrapper> searchFlights(String origin, String destination, LocalDate departureDate, Integer passengers, LocalDate returnDate) {
        logger.info("Flight search request: {} -> {}, departure: {}, passengers: {}",
                origin, destination, departureDate, passengers);

        // Use your existing service - just adapt the response
        var searchResponse = flightService.searchFlights(
                origin, destination, departureDate, returnDate, passengers);

        // Convert to generated wrapper format
        FlightsResponseWrapper response = new FlightsResponseWrapper()
                .status(FlightsResponseWrapper.StatusEnum.SUCCESS)
                .data(convertToGeneratedDTO(searchResponse));

        logger.info("Returning {} flights for search request", response.getData().getFlights().size());
        return ResponseEntity.ok(response);
    }
}
