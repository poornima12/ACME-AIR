package com.acme.air.unit;

import com.acme.air.dto.BookingRequest;
import com.acme.air.dto.BookingResponse;
import com.acme.air.dto.PaymentMethod;
import com.acme.air.dto.PaymentStatus;
import com.acme.air.exception.BookingConflictException;
import com.acme.air.exception.ResourceNotFoundException;
import com.acme.air.exception.SeatUnavailableException;
import com.acme.air.model.*;
import com.acme.air.repository.*;
import com.acme.air.service.BookingIdGenerator;
import com.acme.air.service.BookingService;
import com.acme.air.service.SeatLockService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.test.util.ReflectionTestUtils;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookingServiceTest {

    @Mock
    private BookingRepository bookingRepository;
    @Mock
    private PassengerRepository passengerRepository;
    @Mock
    private FlightScheduleRepository flightScheduleRepository;
    @Mock
    private SeatRepository seatRepository;
    @Mock
    private SeatLockRepository seatLockRepository;
    @Mock
    private PaymentRepository paymentRepository;
    @Mock
    private BookingItemRepository bookingItemRepository;
    @Mock
    private BookingIdGenerator bookingIdGenerator;
    @Mock
    private SeatLockService seatLockService;

    @InjectMocks
    private BookingService bookingService;

    private BookingRequest validRequest;
    private FlightSchedule flightSchedule;
    private Seat seat1, seat2;
    private Passenger passenger1, passenger2;
    private Booking booking;
    private Payment payment;
    private final String sessionId = "session-123";

    @BeforeEach
    void setUp() {
        setupTestData();
        setupValidRequest();
    }

    private void setupTestData() {
        // Flight Schedule
        flightSchedule = new FlightSchedule();
        flightSchedule.setId(1L);
        flightSchedule.setDepartureTime(ZonedDateTime.now().plusHours(5));
        flightSchedule.setTotalSeats(180);

        Flight flight = new Flight();
        flight.setFlightCode("AA123");
        flightSchedule.setFlight(flight);

        // Seats
        seat1 = new Seat();
        seat1.setId(1L);
        seat1.setSeatNumber("12A");
        seat1.setStatus(Seat.SeatStatus.AVAILABLE);

        seat2 = new Seat();
        seat2.setId(2L);
        seat2.setSeatNumber("12B");
        seat2.setStatus(Seat.SeatStatus.AVAILABLE);

        // Passengers
        passenger1 = new Passenger();
        passenger1.setId(1L);
        passenger1.setEmail("john@example.com");
        passenger1.setFirstName("John");
        passenger1.setLastName("Doe");
        passenger1.setPassportNumber("P123456");

        passenger2 = new Passenger();
        passenger2.setId(2L);
        passenger2.setEmail("jane@example.com");
        passenger2.setFirstName("Jane");
        passenger2.setLastName("Smith");
        passenger2.setPassportNumber("P654321");

        // Booking
        booking = new Booking();
        booking.setId(1L);
        booking.setBookingReference("ABC123");
        booking.setStatus(Booking.BookingStatus.CONFIRMED);
        booking.setSchedule(flightSchedule);
        booking.setBookingTime(ZonedDateTime.now());

        // Payment
        payment = new Payment();
        payment.setId(1L);
        payment.setTransactionId("TXN123");
        payment.setTotalAmountPaid(new BigDecimal("299.99"));
        payment.setCurrency("USD");
        payment.setStatus(Payment.PaymentStatus.SUCCESS);
    }

    private void setupValidRequest() {
        BookingRequest.PassengerDTO passengerDTO = new BookingRequest.PassengerDTO(
                "John", "Doe", "john@example.com", "P123456", "12A");
        BookingRequest.PriceDTO priceDTO = new BookingRequest.PriceDTO(
                new BigDecimal("299.99"), "USD");
        BookingRequest.PaymentInfoDTO paymentDTO = new BookingRequest.PaymentInfoDTO(
                PaymentMethod.CREDIT_CARD, "TXN123", priceDTO, PaymentStatus.SUCCESS);

        validRequest = new BookingRequest(1L, List.of(passengerDTO), paymentDTO);
    }


    // ============ VALIDATION EDGE CASES ============

    @Test
    void createBooking_NoPassengers_ThrowsIllegalArgumentException() {
        BookingRequest emptyRequest = new BookingRequest(1L, Collections.emptyList(), validRequest.payment());

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> bookingService.createBooking(emptyRequest, sessionId));
        assertEquals("At least one passenger is required", exception.getMessage());
    }

    @Test
    void createBooking_TooManyPassengers_ThrowsIllegalArgumentException() {
        List<BookingRequest.PassengerDTO> tooManyPassengers = Arrays.asList(
                new BookingRequest.PassengerDTO("P1", "L1", "p1@test.com", "PP1", "1A"),
                new BookingRequest.PassengerDTO("P2", "L2", "p2@test.com", "PP2", "1B"),
                new BookingRequest.PassengerDTO("P3", "L3", "p3@test.com", "PP3", "1C"),
                new BookingRequest.PassengerDTO("P4", "L4", "p4@test.com", "PP4", "1D")
        );
        BookingRequest overloadedRequest = new BookingRequest(1L, tooManyPassengers, validRequest.payment());

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> bookingService.createBooking(overloadedRequest, sessionId));
        assertEquals("Maximum 3 passengers allowed per booking", exception.getMessage());
    }

    @Test
    void createBooking_DuplicateSeatSelection_ThrowsIllegalArgumentException() {
        List<BookingRequest.PassengerDTO> duplicateSeats = Arrays.asList(
                new BookingRequest.PassengerDTO("P1", "L1", "p1@test.com", "PP1", "12A"),
                new BookingRequest.PassengerDTO("P2", "L2", "p2@test.com", "PP2", "12A") // Same seat
        );
        BookingRequest duplicateRequest = new BookingRequest(1L, duplicateSeats, validRequest.payment());

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> bookingService.createBooking(duplicateRequest, sessionId));
        assertEquals("Duplicate seat selection: 12A", exception.getMessage());
    }

    @Test
    void createBooking_InvalidSeatFormat_ThrowsIllegalArgumentException() {
        BookingRequest.PassengerDTO invalidSeat = new BookingRequest.PassengerDTO(
                "John", "Doe", "john@example.com", "P123456", "INVALID");
        BookingRequest invalidRequest = new BookingRequest(1L, List.of(invalidSeat), validRequest.payment());

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> bookingService.createBooking(invalidRequest, sessionId));
        assertEquals("Invalid seat number format: INVALID", exception.getMessage());
    }

    @Test
    void createBooking_EmptySeatSelection_ThrowsIllegalArgumentException() {
        BookingRequest.PassengerDTO emptySeat = new BookingRequest.PassengerDTO(
                "John", "Doe", "john@example.com", "P123456", "");
        BookingRequest invalidRequest = new BookingRequest(1L, List.of(emptySeat), validRequest.payment());

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> bookingService.createBooking(invalidRequest, sessionId));
        assertEquals("Seat selection is required for passenger: John Doe", exception.getMessage());
    }

    @Test
    void createBooking_InvalidSeatNumbers_ThrowsResourceNotFoundException() {
        when(flightScheduleRepository.findById(1L)).thenReturn(Optional.of(flightSchedule));
        when(passengerRepository.findByEmail("john@example.com")).thenReturn(Optional.empty());
        when(passengerRepository.save(any(Passenger.class))).thenReturn(passenger1);
        when(seatRepository.findByScheduleIdAndSeatNumberIn(1L, List.of("12A")))
                .thenReturn(Collections.emptyList()); // No seats found

        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class,
                () -> bookingService.createBooking(validRequest, sessionId));
        assertTrue(exception.getMessage().contains("Invalid seat numbers for this flight"));
    }

    private void setupMultiPassengerBookingMocks() {
        when(flightScheduleRepository.findById(1L)).thenReturn(Optional.of(flightSchedule));
        when(passengerRepository.findByEmail("john@example.com")).thenReturn(Optional.empty());
        when(passengerRepository.findByEmail("jane@example.com")).thenReturn(Optional.empty());
        when(passengerRepository.save(any(Passenger.class))).thenReturn(passenger1, passenger2);
        when(seatRepository.findByScheduleIdAndSeatNumberIn(1L, List.of("12A", "12B")))
                .thenReturn(List.of(seat1, seat2));
        when(seatRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(seat1));
        when(seatRepository.findByIdForUpdate(2L)).thenReturn(Optional.of(seat2));
        when(seatLockRepository.findActiveLockBySeatId(anyLong(), any(LocalDateTime.class)))
                .thenReturn(Optional.empty());
        when(bookingRepository.findByPassengerAndSchedule(anyLong(), eq(1L), eq(Booking.BookingStatus.CONFIRMED)))
                .thenReturn(Optional.empty());
        when(bookingRepository.countConfirmedBookingsBySchedule(1L, Booking.BookingStatus.CONFIRMED))
                .thenReturn(50L);
        when(bookingIdGenerator.generateBookingReference()).thenReturn("ABC123");
        when(bookingRepository.save(any(Booking.class))).thenReturn(booking);
        when(paymentRepository.save(any(Payment.class))).thenReturn(payment);
        when(bookingItemRepository.save(any(BookingItem.class))).thenReturn(new BookingItem());
        when(seatRepository.save(any(Seat.class))).thenReturn(seat1, seat2);

        // Setup booking response data
        BookingItem item1 = new BookingItem();
        item1.setPassenger(passenger1);
        item1.setSeat(seat1);
        BookingItem item2 = new BookingItem();
        item2.setPassenger(passenger2);
        item2.setSeat(seat2);
        booking.setBookingItems(List.of(item1, item2));
        booking.setPayment(payment);
    }

    private void setupSuccessfulBookingMocks() {
        when(flightScheduleRepository.findById(eq(1L))).thenReturn(Optional.of(flightSchedule));
        when(passengerRepository.findByEmail(eq("john@example.com"))).thenReturn(Optional.empty());
        when(passengerRepository.save(any(Passenger.class))).thenReturn(passenger1);
        when(seatRepository.findByScheduleIdAndSeatNumberIn(eq(1L), eq(List.of("12A"))))
                .thenReturn(List.of(seat1));
        when(seatRepository.findByIdForUpdate(eq(1L))).thenReturn(Optional.of(seat1));
        when(seatLockRepository.findActiveLockBySeatId(eq(1L), any(LocalDateTime.class)))
                .thenReturn(Optional.empty());
        when(bookingRepository.findByPassengerAndSchedule(eq(1L), eq(1L), eq(Booking.BookingStatus.CONFIRMED)))
                .thenReturn(Optional.empty());
        when(bookingRepository.countConfirmedBookingsBySchedule(eq(1L), eq(Booking.BookingStatus.CONFIRMED)))
                .thenReturn(50L);
        when(bookingIdGenerator.generateBookingReference()).thenReturn("ABC123");
        when(bookingRepository.save(any(Booking.class))).thenReturn(booking);
        when(paymentRepository.save(any(Payment.class))).thenReturn(payment);
        when(bookingItemRepository.save(any(BookingItem.class))).thenReturn(new BookingItem());
        when(seatRepository.save(any(Seat.class))).thenReturn(seat1);

        // Setup booking response data
        BookingItem bookingItem = new BookingItem();
        bookingItem.setPassenger(passenger1);
        bookingItem.setSeat(seat1);
        booking.setBookingItems(List.of(bookingItem));
        booking.setPayment(payment);
    }
}

