package com.acme.air.component;

import com.acme.air.controller.BookingController;
import com.acme.air.dto.BookingRequest;
import com.acme.air.dto.BookingResponse;
import com.acme.air.exception.SeatUnavailableException;
import com.acme.air.generated.dto.PaymentMethod;
import com.acme.air.generated.dto.PaymentStatus;
import com.acme.air.service.BookingService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;


@WebMvcTest(BookingController.class)
@DisplayName("BookingController Component Tests")
class BookingControllerComponentTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private BookingService bookingService;

    @Test
    @DisplayName("SUCCESS: Should create booking successfully with valid request")
    void shouldCreateBookingSuccessfully() throws Exception {
        // Given
        var generatedBookingRequest = createValidGeneratedBookingRequest();
        var expectedBookingResponse = createMockBookingResponse();

        // Mock the service layer response
        when(bookingService.createBooking(any(BookingRequest.class), anyString()))
                .thenReturn(expectedBookingResponse);

        // When & Then
        mockMvc.perform(post("/api/v1/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(generatedBookingRequest)))
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))

                // Verify response structure
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.data").exists())

                // Verify booking details
                .andExpect(jsonPath("$.data.bookingId").value("AIR1234ABCD"))
                .andExpect(jsonPath("$.data.status").value("CONFIRMED"))
                .andExpect(jsonPath("$.data.flightNumber").value("AC101"))
                .andExpect(jsonPath("$.data.departureDate").exists())

                // Verify passenger details
                .andExpect(jsonPath("$.data.passengers").isArray())
                .andExpect(jsonPath("$.data.passengers", hasSize(2)))
                .andExpect(jsonPath("$.data.passengers[0].firstName").value("John"))
                .andExpect(jsonPath("$.data.passengers[0].lastName").value("Doe"))
                .andExpect(jsonPath("$.data.passengers[0].email").value("john.doe@example.com"))
                .andExpect(jsonPath("$.data.passengers[0].seatNumber").value("12A"))
                .andExpect(jsonPath("$.data.passengers[1].firstName").value("Jane"))
                .andExpect(jsonPath("$.data.passengers[1].lastName").value("Smith"))
                .andExpect(jsonPath("$.data.passengers[1].email").value("jane.smith@example.com"))
                .andExpect(jsonPath("$.data.passengers[1].seatNumber").value("12B"))

                // Verify payment details
                .andExpect(jsonPath("$.data.payment").exists())
                .andExpect(jsonPath("$.data.payment.transactionId").value("TXN123456"))
                .andExpect(jsonPath("$.data.payment.amountPaid").value(599.98))
                .andExpect(jsonPath("$.data.payment.currency").value("NZD"));
    }

    @Test
    @DisplayName("FAILURE: Should return 409 when seats are unavailable")
    void shouldReturn409WhenSeatsUnavailable() throws Exception {
        // Given
        var generatedBookingRequest = createValidGeneratedBookingRequest();

        // Mock service to throw SeatUnavailableException
        when(bookingService.createBooking(any(BookingRequest.class), anyString()))
                .thenThrow(new SeatUnavailableException("Seat 12A is not available"));

        // When & Then
        mockMvc.perform(post("/api/v1/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(generatedBookingRequest)))
                .andDo(print())
                .andExpect(status().isConflict())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))

                // Verify error response structure
                .andExpect(jsonPath("$.status").value("ERROR"))
                .andExpect(jsonPath("$.error").exists())
                .andExpect(jsonPath("$.error.code").value("SEAT_UNAVAILABLE"))
                .andExpect(jsonPath("$.error.message").value("Seat 12A is not available"))
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    // Helper methods to create test data
    private com.acme.air.generated.dto.BookingRequest createValidGeneratedBookingRequest() {
        var passenger1 = new com.acme.air.generated.dto.PassengerDTO();
        passenger1.setFirstName("John");
        passenger1.setLastName("Doe");
        passenger1.setEmail("john.doe@example.com");
        passenger1.setPassportNumber("P123456789");
        passenger1.setSelectedSeatNumber("12A");

        var passenger2 = new com.acme.air.generated.dto.PassengerDTO();
        passenger2.setFirstName("Jane");
        passenger2.setLastName("Smith");
        passenger2.setEmail("jane.smith@example.com");
        passenger2.setPassportNumber("P987654321");
        passenger2.setSelectedSeatNumber("12B");

        var priceInfo = new com.acme.air.generated.dto.PriceDTO();
        priceInfo.setAmountPaid(new BigDecimal("599.98"));
        priceInfo.setCurrency("NZD");

        var paymentInfo = new com.acme.air.generated.dto.PaymentInfoDTO();
        paymentInfo.setMethod(PaymentMethod.CREDIT_CARD);
        paymentInfo.setTransactionId("TXN123456");
        paymentInfo.setStatus(PaymentStatus.SUCCESS);
        paymentInfo.setPrice(priceInfo);

        var bookingRequest = new com.acme.air.generated.dto.BookingRequest();
        bookingRequest.setFlightScheduleId(1L);
        bookingRequest.setPassengers(List.of(passenger1, passenger2));
        bookingRequest.setPayment(paymentInfo);

        return bookingRequest;
    }

    private BookingResponse createMockBookingResponse() {
        var passengerSeat1 = new BookingResponse.PassengerSeatDTO(
                "John", "Doe", "john.doe@example.com", "12A");
        var passengerSeat2 = new BookingResponse.PassengerSeatDTO(
                "Jane", "Smith", "jane.smith@example.com", "12B");

        var paymentInfo = new BookingResponse.PaymentInfoDTO(
                "TXN123456", new BigDecimal("599.98"), "NZD");

        return new BookingResponse(
                "AIR1234ABCD",
                "CONFIRMED",
                "AC101",
                ZonedDateTime.now().plusDays(7),
                List.of(passengerSeat1, passengerSeat2),
                paymentInfo,
                LocalDate.now()
        );
    }
}
