package com.acme.air.controller;

import com.acme.air.dto.ApiResponse;
import com.acme.air.dto.FlightSearchResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.ArrayList;

@RestController
@RequestMapping("/api/v1/flights")
public class FlightController {

    private static final Logger logger = LoggerFactory.getLogger(FlightController.class);

    @GetMapping("/search")
    public ResponseEntity<ApiResponse<FlightSearchResponse>> searchFlights(
            @RequestParam String origin,
            @RequestParam String destination,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate departureDate, //YYYY-MM-DD
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate returnDate,
            @RequestParam int passengers,
            @RequestParam(required = false) String cabinClass,
            @RequestParam(required = false) Boolean nonStopOnly
    ) {
        // TODO: Implement search logic
        logger.info("Search request received: {}", origin);
        FlightSearchResponse response = new FlightSearchResponse(new ArrayList<>());
        return ResponseEntity.ok(new ApiResponse<>("SUCCESS", response));
    }
}
