package com.acme.air.controller;

import com.acme.air.api.BookingsApi;
import com.acme.air.dto.ApiResponse;
import com.acme.air.dto.BookingRequest;
import com.acme.air.dto.BookingResponse;
import com.acme.air.exception.BookingConflictException;
import com.acme.air.exception.SeatUnavailableException;
import com.acme.air.generated.dto.BookingResponseWrapper;
import com.acme.air.service.BookingService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.RandomUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

import static com.acme.air.mapper.DTOMapper.convertToExistingDTO;
import static com.acme.air.mapper.DTOMapper.convertToGeneratedBookingResponse;
import static org.hibernate.annotations.UuidGenerator.Style.RANDOM;
import static org.springframework.web.util.WebUtils.getSessionId;

@RestController
@RequestMapping("/api/v1/bookings")
@RequiredArgsConstructor
public class BookingController implements BookingsApi {

    private static final Logger log = LoggerFactory.getLogger(BookingController.class);

    private final BookingService bookingService;

    @Override
    @PostMapping
    public ResponseEntity<BookingResponseWrapper> createBooking(com.acme.air.generated.dto.BookingRequest bookingRequest) {
        var existingBookingRequest = convertToExistingDTO(bookingRequest);
        String sessionId = getCurrentSessionId(); // You'll need to handle session differently
        var bookingResponse = bookingService.createBooking(existingBookingRequest, sessionId);

        // Convert response back to generated DTO
        BookingResponseWrapper response = new BookingResponseWrapper()
                .status(BookingResponseWrapper.StatusEnum.SUCCESS)
                .data(convertToGeneratedBookingResponse(bookingResponse));

        log.info("Successfully created booking {} for {} passengers",
                bookingResponse.bookingId(), bookingResponse.passengers().size());

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    private String getCurrentSessionId() {
        // Replace with actual session management after authentication is implemented
        return "session-" + UUID.randomUUID().toString().substring(0, 8);
    }
}
