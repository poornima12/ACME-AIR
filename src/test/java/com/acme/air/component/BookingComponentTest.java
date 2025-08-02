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

    private MockHttpSession mockSession;


    private BookingRequest validBookingRequest;
    private BookingResponse mockBookingResponse;

    @BeforeEach
    void setUp() {
        // Setup valid booking request
        BookingRequest.PassengerDTO passenger1 = new BookingRequest.PassengerDTO(
                "John", "Doe", "john.doe@example.com", "P12345678", "12A"
        );

        BookingRequest.PassengerDTO passenger2 = new BookingRequest.PassengerDTO(
                "Jane", "Smith", "jane.smith@example.com", "P87654321", "12B"
        );

        BookingRequest.PriceDTO price = new BookingRequest.PriceDTO(
                BigDecimal.valueOf(598.00), "NZD"
        );

        BookingRequest.PaymentInfoDTO payment = new BookingRequest.PaymentInfoDTO(
                PaymentMethod.CREDIT_CARD,
                "TXN_123456789",
                price,
                PaymentStatus.SUCCESS
        );

        validBookingRequest = new BookingRequest(
                1L, Arrays.asList(passenger1, passenger2), payment
        );

        HttpSession mockSession = Mockito.mock(HttpSession.class);
        Mockito.when(mockSession.getId()).thenReturn("test-session-123");
        // Setup mock response
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

    @Test
    @DisplayName("SUCCESS: Should create booking successfully with valid request")
    void createBooking_ValidRequest_ReturnsBookingResponse() throws Exception {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("test", "value");
        // Given
        when(bookingService.createBooking(any(BookingRequest.class), anyString()))
                .thenReturn(mockBookingResponse);

        // When & Then
        mockMvc.perform(post("/api/v1/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validBookingRequest))
                        .session(session))
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

        verify(bookingService).createBooking(any(BookingRequest.class), anyString());
    }

    @Test
    @DisplayName("FAILURE: Should return 409 when seats are unavailable")
    void createBooking_SeatsUnavailable_Returns409Conflict() throws Exception {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("test", "value");

        // Given - Mock service to throw SeatUnavailableException
        when(bookingService.createBooking(any(BookingRequest.class), anyString()))
                .thenThrow(new SeatUnavailableException("Seat 12A is not available"));

        // When & Then
        mockMvc.perform(post("/api/v1/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validBookingRequest))
                        .session(session))
                .andExpect(status().isConflict()) // 409 Conflict
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status", is("ERROR")))
                .andExpect(jsonPath("$.error.code", is("SEAT_UNAVAILABLE")))
                .andExpect(jsonPath("$.error.message", containsString("Seat 12A is not available")));

        verify(bookingService).createBooking(any(BookingRequest.class), anyString());
    }

}
