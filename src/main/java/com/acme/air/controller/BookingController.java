package com.acme.air.controller;

import com.acme.air.dto.ApiResponse;
import com.acme.air.dto.BookingRequest;
import com.acme.air.dto.BookingResponse;
import com.acme.air.exception.BookingConflictException;
import com.acme.air.exception.SeatUnavailableException;
import com.acme.air.service.BookingService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.web.util.WebUtils.getSessionId;

@RestController
@RequestMapping("/api/v1/bookings")
@RequiredArgsConstructor
@Slf4j
public class BookingController {

    private final BookingService bookingService;

    @PostMapping
    public ResponseEntity<ApiResponse<BookingResponse>> createBooking(@RequestBody @Valid BookingRequest bookingRequest,
                                                                      HttpServletRequest request) {

            String sessionId = getSessionId(request);
            BookingResponse booking = bookingService.createBooking(bookingRequest, sessionId);
            log.info("Successfully created booking {} for {} passengers",
                    booking.bookingId(), booking.passengers().size());
            return ResponseEntity.status(HttpStatus.CREATED).body(new ApiResponse<>(booking));
        }
}
