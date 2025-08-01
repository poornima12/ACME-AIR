package com.acme.air.controller;

import com.acme.air.dto.ApiResponse;
import com.acme.air.dto.BookingRequest;
import com.acme.air.dto.BookingResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/bookings")
public class BookingController {

    @PostMapping
    public ResponseEntity<ApiResponse<BookingResponse>> createBooking(@RequestBody BookingRequest bookingRequest) {
        // TODO: Implement booking logic
        return ResponseEntity.ok().build();
    }
}
