package com.acme.air.controller;

import com.acme.air.dto.ApiResponse;
import com.acme.air.dto.BookingRequest;
import com.acme.air.dto.BookingResponse;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/bookings")
public class BookingController {

    private static final Logger logger = LoggerFactory.getLogger(BookingController.class);

    @PostMapping
    public ResponseEntity<ApiResponse<BookingResponse>> createBooking(@RequestBody @Valid BookingRequest bookingRequest) {
        // TODO: Implement booking logic
        logger.info("Booking request received: {}", bookingRequest);
        return ResponseEntity.ok().build();
    }
}
