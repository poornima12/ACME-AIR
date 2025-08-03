package com.acme.air.component;

import com.acme.air.controller.BookingController;
import com.acme.air.dto.BookingRequest;
import com.acme.air.dto.BookingResponse;
import com.acme.air.dto.PaymentMethod;
import com.acme.air.dto.PaymentStatus;
import com.acme.air.exception.SeatUnavailableException;
import com.acme.air.model.Payment;
import com.acme.air.service.BookingService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(BookingController.class)
@DisplayName("Booking Component Tests")
class BookingComponentTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private BookingService bookingService;

    @Autowired
    private ObjectMapper objectMapper;

    // Use generated DTO for request (what the API actually expects)
    private com.acme.air.generated.dto.BookingRequest validGeneratedBookingRequest;

    // Use existing DTO for service response (what your service returns)
    private BookingResponse mockBookingResponse;

    @BeforeEach
    void setUp() {
        setupGeneratedBookingRequest();
        setupMockBookingResponse();
    }

    private void setupGeneratedBookingRequest() {
        // Create passengers using generated DTOs
        var passenger1 = new com.acme.air.generated.dto.PassengerDTO()
                .firstName("John")
                .lastName("Doe")
                .email("john.doe@example.com")
                .passportNumber("P12345678")
                .selectedSeatNumber("12A");

        var passenger2 = new com.acme.air.generated.dto.PassengerDTO()
                .firstName("Jane")
                .lastName("Smith")
                .email("jane.smith@example.com")
                .passportNumber("P87654321")
                .selectedSeatNumber("12B");

        // Create price using generated DTO
        var price = new com.acme.air.generated.dto.PriceDTO()
                .amountPaid(BigDecimal.valueOf(598.00))
                .currency("NZD");

        // Create payment using generated DTO
        var payment = new com.acme.air.generated.dto.PaymentInfoDTO()
                .method(com.acme.air.generated.dto.PaymentMethod.CREDIT_CARD)
                .transactionId("TXN_123456789")
                .price(price)
                .status(com.acme.air.generated.dto.PaymentStatus.SUCCESS);

        // Create the main request using generated DTO
        validGeneratedBookingRequest = new com.acme.air.generated.dto.BookingRequest()
                .flightScheduleId(1L)
                .passengers(Arrays.asList(passenger1, passenger2))
                .payment(payment);
    }

    private void setupMockBookingResponse() {
        // Setup mock response using your existing DTOs (what service returns)
        BookingResponse.PassengerSeatDTO passengerSeat1 = new BookingResponse.PassengerSeatDTO(
                "John", "Doe", "john.doe@example.com", "12A"
        );

        BookingResponse.PassengerSeatDTO passengerSeat2 = new BookingResponse.PassengerSeatDTO(
                "Jane", "Smith", "jane.smith@example.com", "12B"
        );

        BookingResponse.PaymentInfoDTO paymentInfo = new BookingResponse.PaymentInfoDTO(
                "TXN_123456789", BigDecimal.valueOf(598.00), "NZD"
        );

        mockBookingResponse = new BookingResponse(
                "AIR1234ABCD",
                "CONFIRMED",
                "NZ123",
                ZonedDateTime.now().plusDays(1),
                Arrays.asList(passengerSeat1, passengerSeat2),
                paymentInfo,
                LocalDate.now()
        );
    }

    @DisplayName("SUCCESS: Should create booking successfully with valid request")
    void createBooking_ValidRequest_ReturnsBookingResponse() throws Exception {
        MockHttpSession session = new MockHttpSession();

        // Given - Mock service to return response
        // Note: The service receives your existing DTO (after conversion) and a session ID
        when(bookingService.createBooking(any(BookingRequest.class), anyString()))
                .thenReturn(mockBookingResponse);

        // When & Then - Send generated DTO format (what the API expects)
        mockMvc.perform(post("/api/v1/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validGeneratedBookingRequest))
                        .session(session))
                .andExpect(status().isCreated()) // 201 Created
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status", is("SUCCESS")))
                .andExpect(jsonPath("$.data.bookingId", is("AIR1234ABCD")))
                .andExpect(jsonPath("$.data.status", is("CONFIRMED")))
                .andExpect(jsonPath("$.data.flightNumber", is("NZ123")))
                .andExpect(jsonPath("$.data.passengers", hasSize(2)))
                .andExpect(jsonPath("$.data.passengers[0].firstName", is("John")))
                .andExpect(jsonPath("$.data.passengers[0].lastName", is("Doe")))
                .andExpect(jsonPath("$.data.passengers[0].email", is("john.doe@example.com")))
                .andExpect(jsonPath("$.data.passengers[0].seatNumber", is("12A")))
                .andExpect(jsonPath("$.data.passengers[1].firstName", is("Jane")))
                .andExpect(jsonPath("$.data.passengers[1].lastName", is("Smith")))
                .andExpect(jsonPath("$.data.passengers[1].email", is("jane.smith@example.com")))
                .andExpect(jsonPath("$.data.passengers[1].seatNumber", is("12B")))
                .andExpect(jsonPath("$.data.payment.transactionId", is("TXN_123456789")))
                .andExpect(jsonPath("$.data.payment.amountPaid", is(598.00)))
                .andExpect(jsonPath("$.data.payment.currency", is("NZD")));

        // Verify service was called with converted DTO and some session ID
        verify(bookingService).createBooking(any(BookingRequest.class), anyString());
    }

    @DisplayName("FAILURE: Should return 409 when seats are unavailable")
    void createBooking_SeatsUnavailable_Returns409Conflict() throws Exception {
        MockHttpSession session = new MockHttpSession();

        // Given - Mock service to throw SeatUnavailableException
        when(bookingService.createBooking(any(BookingRequest.class), anyString()))
                .thenThrow(new SeatUnavailableException("Seat 12A is not available"));

        // When & Then - Send generated DTO format (what the API expects)
        mockMvc.perform(post("/api/v1/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validGeneratedBookingRequest))
                        .session(session))
                .andExpect(status().isConflict()) // 409 Conflict
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status", is("ERROR")))
                .andExpect(jsonPath("$.error.code", is("SEAT_UNAVAILABLE")))
                .andExpect(jsonPath("$.error.message", containsString("Seat 12A is not available")));

        verify(bookingService).createBooking(any(BookingRequest.class), anyString());
    }
}
