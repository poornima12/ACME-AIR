package com.acme.air.unit;

import com.acme.air.dto.BookingRequest;
import com.acme.air.dto.BookingResponse;
import com.acme.air.dto.PaymentMethod;
import com.acme.air.dto.PaymentStatus;
import com.acme.air.exception.BookingConflictException;
import com.acme.air.exception.SeatUnavailableException;
import com.acme.air.model.*;
import com.acme.air.repository.*;
import com.acme.air.service.BookingIdGenerator;
import com.acme.air.service.BookingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.*;
import org.testcontainers.shaded.com.google.common.base.Verify;

@SpringBootTest
@Testcontainers
@Transactional
class BookingServiceIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test")
            .withReuse(false)  // Disable reuse in build
            .withStartupTimeoutSeconds(120); // Increase timeout for slow build environments

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
    }

    @Autowired
    private BookingService bookingService;

    @MockBean
    private BookingIdGenerator bookingIdGenerator;

    @Autowired
    private BookingRepository bookingRepository;
    @Autowired
    private FlightScheduleRepository flightScheduleRepository;
    @Autowired
    private SeatRepository seatRepository;
    @Autowired
    private FlightRepository flightRepository;
    @Autowired
    AirportRepository airportRepository;
    @Autowired
    PassengerRepository passengerRepository;
    @Autowired
    SeatLockRepository seatLockRepository;

    private BookingRequest validRequest;
    private FlightSchedule flightSchedule;
    private final String sessionId = "session-123";

    @BeforeEach
    void setUp() {
        setupTestData();
        setupValidRequest();
        when(bookingIdGenerator.generateBookingReference()).thenReturn("ABC123");
    }

    private void setupTestData() {
        Airport origin = new Airport();
        origin.setCode("NYC");
        origin.setName("New York City");
        origin.setCity("New York");
        airportRepository.save(origin);

        Airport destination = new Airport();
        destination.setCode("LAX");
        destination.setName("Los Angeles");
        destination.setCity("Los Angeles");
        airportRepository.save(destination);

        Flight newFlight = new Flight();
        newFlight.setFlightCode("AA123");
        newFlight.setOrigin(origin);
        newFlight.setDestination(destination);
        newFlight.setAirline("American Airlines");
        flightRepository.save(newFlight);

        // Option 2: Create minimal entities
        flightSchedule = new FlightSchedule();
        flightSchedule.setDepartureTime(ZonedDateTime.now().plusHours(5));
        flightSchedule.setTotalSeats(180);
        flightSchedule.setFlight(newFlight);
        flightSchedule = flightScheduleRepository.save(flightSchedule);

        // Create seat
        Seat seat1 = new Seat();
        seat1.setSeatNumber("12A");
        seat1.setStatus(Seat.SeatStatus.AVAILABLE);
        seat1.setSchedule(flightSchedule);
        seatRepository.save(seat1);
    }

    private void setupValidRequest() {
        BookingRequest.PassengerDTO passengerDTO = new BookingRequest.PassengerDTO(
                "John", "Doe", "john@example.com", "P123456", "12A");
        BookingRequest.PriceDTO priceDTO = new BookingRequest.PriceDTO(
                new BigDecimal("299.99"), "USD");
        BookingRequest.PaymentInfoDTO paymentDTO = new BookingRequest.PaymentInfoDTO(
                PaymentMethod.CREDIT_CARD, "TXN123", priceDTO, PaymentStatus.SUCCESS);

        validRequest = new BookingRequest(flightSchedule.getId(), List.of(passengerDTO), paymentDTO);
    }

    @Test
    void createBooking_ValidSinglePassenger_Success() {
        // Act
        BookingResponse response = bookingService.createBooking(validRequest, sessionId);

        // Assert
        assertNotNull(response);
        assertEquals("ABC123", response.bookingId());
        assertEquals("CONFIRMED", response.status());

        // Verify data was persisted
        Optional<Booking> savedBooking = bookingRepository.findByBookingReference("ABC123");
        assertTrue(savedBooking.isPresent());
        assertEquals(Booking.BookingStatus.CONFIRMED, savedBooking.get().getStatus());
    }

    @Test
    void createBooking_ValidMultiplePassengers_Success() {
        // Arrange - Create additional seats for multiple passengers
        Seat seat2 = new Seat();
        seat2.setSeatNumber("12B");
        seat2.setStatus(Seat.SeatStatus.AVAILABLE);
        seat2.setSchedule(flightSchedule);
        seatRepository.save(seat2);

        Seat seat3 = new Seat();
        seat3.setSeatNumber("12C");
        seat3.setStatus(Seat.SeatStatus.AVAILABLE);
        seat3.setSchedule(flightSchedule);
        seatRepository.save(seat3);

        // Create multiple passengers
        BookingRequest.PassengerDTO passenger1 = new BookingRequest.PassengerDTO(
                "John", "Doe", "john@example.com", "P123456", "12A");
        BookingRequest.PassengerDTO passenger2 = new BookingRequest.PassengerDTO(
                "Jane", "Smith", "jane@example.com", "P789012", "12B");
        BookingRequest.PassengerDTO passenger3 = new BookingRequest.PassengerDTO(
                "Bob", "Johnson", "bob@example.com", "P345678", "12C");

        List<BookingRequest.PassengerDTO> passengers = List.of(passenger1, passenger2, passenger3);

        // Create price for multiple passengers (assuming price is total for all passengers)
        BookingRequest.PriceDTO priceDTO = new BookingRequest.PriceDTO(
                new BigDecimal("899.97"), "USD"); // 3 passengers * 299.99
        BookingRequest.PaymentInfoDTO paymentDTO = new BookingRequest.PaymentInfoDTO(
                PaymentMethod.CREDIT_CARD, "TXN456", priceDTO, PaymentStatus.SUCCESS);

        BookingRequest multiPassengerRequest = new BookingRequest(
                flightSchedule.getId(), passengers, paymentDTO);

        // Act
        BookingResponse response = bookingService.createBooking(multiPassengerRequest, sessionId);

        // Assert
        assertNotNull(response);
        assertEquals("ABC123", response.bookingId());
        assertEquals("CONFIRMED", response.status());

        // Verify booking was persisted
        Optional<Booking> savedBooking = bookingRepository.findByBookingReference("ABC123");
        assertTrue(savedBooking.isPresent());

        Booking booking = savedBooking.get();
        assertEquals(Booking.BookingStatus.CONFIRMED, booking.getStatus());
        assertEquals(flightSchedule.getId(), booking.getSchedule().getId());

        // Verify all passengers were booked
        assertNotNull(booking.getBookingItems());
        assertEquals(3, booking.getBookingItems().size());

        // Verify each passenger has correct seat assignment
        List<String> expectedSeatNumbers = List.of("12A", "12B", "12C");
        List<String> actualSeatNumbers = booking.getBookingItems().stream()
                .map(item -> item.getSeat().getSeatNumber())
                .sorted()
                .toList();
        assertEquals(expectedSeatNumbers, actualSeatNumbers);

        // Verify passenger details
        List<String> expectedEmails = Stream.of("john@example.com", "jane@example.com", "bob@example.com").sorted().toList();
        List<String> actualEmails = booking.getBookingItems().stream()
                .map(item -> item.getPassenger().getEmail())
                .sorted()
                .toList();
        assertEquals(expectedEmails, actualEmails);
    }

    @Test
    void createBooking_PassengerAlreadyBookedOnFlight_ThrowsBookingConflictException() {
        // Arrange - First, create a successful booking
        BookingResponse firstBooking = bookingService.createBooking(validRequest, sessionId);
        assertNotNull(firstBooking);
        assertEquals("CONFIRMED", firstBooking.status());

        // Create a second seat for the duplicate booking attempt
        Seat seat2 = new Seat();
        seat2.setSeatNumber("13A");
        seat2.setStatus(Seat.SeatStatus.AVAILABLE);
        seat2.setSchedule(flightSchedule);
        seatRepository.save(seat2);

        // Arrange - Try to book the same passenger on the same flight again
        // With the fixed service logic, this should now reach the BookingConflictException
        BookingRequest.PassengerDTO duplicatePassenger = new BookingRequest.PassengerDTO(
                "John", "Doe", "john@example.com", "P123456", "13A"); // Same passenger details

        BookingRequest.PriceDTO priceDTO = new BookingRequest.PriceDTO(
                new BigDecimal("299.99"), "USD");
        BookingRequest.PaymentInfoDTO paymentDTO = new BookingRequest.PaymentInfoDTO(
                PaymentMethod.CREDIT_CARD, "TXN789", priceDTO, PaymentStatus.SUCCESS);

        BookingRequest duplicateRequest = new BookingRequest(
                flightSchedule.getId(), List.of(duplicatePassenger), paymentDTO);

        String newSessionId = "session-456";

        // Act & Assert - Now expect BookingConflictException for duplicate booking on same flight
        BookingConflictException exception = assertThrows(
                BookingConflictException.class,
                () -> bookingService.createBooking(duplicateRequest, newSessionId)
        );

        // Verify the exception message mentions the duplicate booking
        assertNotNull(exception.getMessage());
        assertTrue(exception.getMessage().contains("already has a confirmed booking"),
                "Exception message should mention existing booking: " + exception.getMessage());
        assertTrue(exception.getMessage().contains("john@example.com"),
                "Exception message should contain the passenger email: " + exception.getMessage());

        // Verify that only one booking exists for this passenger on this flight
        List<Booking> allBookings = bookingRepository.findAll();
        long confirmedBookingsForFlight = allBookings.stream()
                .filter(booking -> booking.getSchedule().getId().equals(flightSchedule.getId()))
                .filter(booking -> booking.getStatus() == Booking.BookingStatus.CONFIRMED)
                .count();
        assertEquals(1, confirmedBookingsForFlight,
                "Should only have one confirmed booking for this flight");

        // Verify the second seat remains available (booking was rejected)
        Optional<Seat> seat2Status = seatRepository.findById(seat2.getId());
        assertTrue(seat2Status.isPresent());
        assertEquals(Seat.SeatStatus.AVAILABLE, seat2Status.get().getStatus(),
                "Second seat should remain available after failed booking");

        // Verify only one passenger record exists (reused existing passenger)
        List<Passenger> allPassengers = passengerRepository.findAll();
        assertEquals(1, allPassengers.size(),
                "Should only have one passenger record (existing one reused)");
        assertEquals("john@example.com", allPassengers.get(0).getEmail());

        // Verify first booking is still intact
        Optional<Booking> firstBookingCheck = bookingRepository.findByBookingReference(firstBooking.bookingId());
        assertTrue(firstBookingCheck.isPresent());
        assertEquals(Booking.BookingStatus.CONFIRMED, firstBookingCheck.get().getStatus());
    }
    @Test
    void createBooking_SeatLockedByAnotherSession_ThrowsSeatUnavailableException() {
        // Arrange - Create an active seat lock by another session
        String otherSessionId = "other-session-789";
        String currentSessionId = "current-session-123";

        // Get the seat that we'll try to book
        Seat targetSeat = seatRepository.findAll().stream()
                .filter(seat -> "12A".equals(seat.getSeatNumber()))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Test seat 12A not found"));

        // Create a passenger for the other session's lock
        Passenger otherPassenger = new Passenger();
        otherPassenger.setFirstName("Other");
        otherPassenger.setLastName("User");
        otherPassenger.setEmail("other@example.com");
        otherPassenger.setPassportNumber("P999999");
        otherPassenger = passengerRepository.save(otherPassenger);

        // Create an active seat lock by another session
        SeatLock activeLock = new SeatLock();
        activeLock.setSessionId(otherSessionId);
        activeLock.setSeat(targetSeat);
        activeLock.setPassenger(otherPassenger);
        activeLock.setLockedAt(LocalDateTime.now().minusMinutes(5)); // Locked 5 minutes ago
        activeLock.setExpiresAt(LocalDateTime.now().plusMinutes(5)); // Expires in 5 minutes
        activeLock.setStatus(SeatLock.LockStatus.ACTIVE);
        seatLockRepository.save(activeLock);

        // Set the seat status to LOCKED (as it would be in real scenario)
        targetSeat.setStatus(Seat.SeatStatus.LOCKED);
        seatRepository.save(targetSeat);

        // Create a booking request for the locked seat
        BookingRequest.PassengerDTO passengerDTO = new BookingRequest.PassengerDTO(
                "Jane", "Smith", "jane.smith@example.com", "P654321", "12A");
        BookingRequest.PriceDTO priceDTO = new BookingRequest.PriceDTO(
                new BigDecimal("299.99"), "USD");
        BookingRequest.PaymentInfoDTO paymentDTO = new BookingRequest.PaymentInfoDTO(
                PaymentMethod.CREDIT_CARD, "TXN999", priceDTO, PaymentStatus.SUCCESS);

        BookingRequest conflictRequest = new BookingRequest(
                flightSchedule.getId(), List.of(passengerDTO), paymentDTO);

        // Act & Assert - Expect SeatUnavailableException
        SeatUnavailableException exception = assertThrows(
                SeatUnavailableException.class,
                () -> bookingService.createBooking(conflictRequest, currentSessionId)
        );

        // Verify the exception message mentions the seat being temporarily reserved
        assertNotNull(exception.getMessage());
        assertTrue(exception.getMessage().contains("temporarily reserved by another user") ||
                        exception.getMessage().contains("not available"),
                "Exception message should mention seat unavailability: " + exception.getMessage());
        assertTrue(exception.getMessage().contains("12A"),
                "Exception message should contain the seat number: " + exception.getMessage());

        // Verify the seat remains locked by the other session
        Optional<Seat> seatCheck = seatRepository.findById(targetSeat.getId());
        assertTrue(seatCheck.isPresent());
        assertEquals(Seat.SeatStatus.LOCKED, seatCheck.get().getStatus(),
                "Seat should remain locked after failed booking attempt");

        // Verify the active lock still exists and belongs to the other session
        Optional<SeatLock> lockCheck = seatLockRepository.findActiveLockBySeatId(
                targetSeat.getId(), LocalDateTime.now());
        assertTrue(lockCheck.isPresent(), "Active lock should still exist");
        assertEquals(otherSessionId, lockCheck.get().getSessionId(),
                "Lock should still belong to the other session");
        assertEquals(SeatLock.LockStatus.ACTIVE, lockCheck.get().getStatus(),
                "Lock should remain active");

        // Verify no booking was created
        List<Booking> allBookings = bookingRepository.findAll();
        assertTrue(allBookings.isEmpty(), "No booking should have been created");

        // Verify the new passenger was created but booking failed
        // (Service creates passenger before checking seat locks)
        Optional<Passenger> newPassengerCheck = passengerRepository.findByEmail("jane.smith@example.com");
        assertTrue(newPassengerCheck.isPresent(),
                "New passenger should have been created before seat lock check failed");

        // Verify both passengers exist (original lock holder + failed booking passenger)
        List<Passenger> allPassengers = passengerRepository.findAll();
        assertEquals(2, allPassengers.size(), "Should have both the lock-holding passenger and the failed booking passenger");

        // Verify passenger emails
        Set<String> passengerEmails = allPassengers.stream()
                .map(Passenger::getEmail)
                .collect(Collectors.toSet());
        assertTrue(passengerEmails.contains("other@example.com"), "Should contain lock-holding passenger");
        assertTrue(passengerEmails.contains("jane.smith@example.com"), "Should contain failed booking passenger");
    }
}
