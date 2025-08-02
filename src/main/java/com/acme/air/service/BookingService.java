package com.acme.air.service;

import com.acme.air.dto.BookingRequest;
import com.acme.air.dto.BookingResponse;
import com.acme.air.exception.BookingConflictException;
import com.acme.air.exception.ResourceNotFoundException;
import com.acme.air.exception.SeatUnavailableException;
import com.acme.air.model.*;
import com.acme.air.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class BookingService {

    private final BookingRepository bookingRepository;
    private final PassengerRepository passengerRepository;
    private final FlightScheduleRepository flightScheduleRepository;
    private final SeatRepository seatRepository;
    private final SeatLockRepository seatLockRepository;
    private final PaymentRepository paymentRepository;
    private final BookingItemRepository bookingItemRepository;
    private final BookingIdGenerator bookingIdGenerator;
    private final SeatLockService seatLockService;

    private static final int MAX_PASSENGERS_PER_BOOKING = 3;
    private static final int MIN_BOOKING_HOURS_BEFORE_DEPARTURE = 2;

    /**
     * Creates a new booking for one or more passengers on a flight
     * Handles all edge cases including seat conflicts, double bookings, flight validation
     */
    @Transactional(isolation = Isolation.SERIALIZABLE, propagation = Propagation.REQUIRED,
            rollbackFor = Exception.class)
    public BookingResponse createBooking(BookingRequest request, String sessionId) {
        log.info("Creating booking for flight schedule {} with {} passengers",
                request.flightScheduleId(), request.passengers().size());

        try {
            // All validation and business logic here...
            validateBookingRequest(request);
            FlightSchedule schedule = getAndValidateFlightSchedule(request.flightScheduleId());
            List<Passenger> passengers = processPassengers(request.passengers());
            List<Seat> seats = validateAndLockSeats(request, schedule, sessionId);
            validateNoDuplicateBookings(passengers, schedule);
            validateFlightCapacity(schedule, seats.size());

            // Create booking (participates in main transaction)
            Booking booking = createBookingForPassengers(passengers, seats, schedule, request);
            Payment payment = createPaymentRecord(booking, request.payment());
            booking.setPayment(payment);
            confirmSeatsAndReleaseLocks(seats, sessionId);

            // Send async confirmation notifications

            BookingResponse response = buildBookingResponse(booking);
            log.info("Successfully created booking {} for {} passengers on flight {}",
                    response.bookingId(), passengers.size(), schedule.getFlight().getFlightCode());

            return response;

        } catch (Exception e) {
            log.error("Failed to create booking for flight schedule {}: {}",
                    request.flightScheduleId(), e.getMessage(), e);
            // Cleanup: release any locks that might have been created
            cleanupFailedBooking(request, sessionId);
            throw e;
        }
    }

    private void validateBookingRequest(BookingRequest request) {
        if (request.passengers() == null || request.passengers().isEmpty()) {
            throw new IllegalArgumentException("At least one passenger is required");
        }
        if (request.passengers().size() > MAX_PASSENGERS_PER_BOOKING) {
            throw new IllegalArgumentException(
                    String.format("Maximum %d passengers allowed per booking", MAX_PASSENGERS_PER_BOOKING));
        }
        validateSeatSelections(request.passengers());
    }

    private void validateSeatSelections(List<BookingRequest.PassengerDTO> passengers) {
        Set<String> seatNumbers = new HashSet<>();
        for (BookingRequest.PassengerDTO passenger : passengers) {
            if (passenger.selectedSeatNumber() == null || passenger.selectedSeatNumber().trim().isEmpty()) {
                throw new IllegalArgumentException("Seat selection is required for passenger: " + passenger.firstName() + " " + passenger.lastName());
            }
            // Validate seat number format (e.g., 12A, 5F)
            if (!passenger.selectedSeatNumber().matches("^[1-9]\\d*[A-Z]$")) {
                throw new IllegalArgumentException("Invalid seat number format: " + passenger.selectedSeatNumber());
            }
            if (!seatNumbers.add(passenger.selectedSeatNumber().toUpperCase())) {
                throw new IllegalArgumentException("Duplicate seat selection: " + passenger.selectedSeatNumber());
            }
        }
    }

    private FlightSchedule getAndValidateFlightSchedule(Long scheduleId) {
        FlightSchedule schedule = flightScheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new ResourceNotFoundException("Flight schedule not found: " + scheduleId));

        ZonedDateTime now = ZonedDateTime.now();
        if (schedule.getDepartureTime().isBefore(now)) {
            throw new IllegalArgumentException("Cannot book flights that have already departed");
        }
        // Check booking window (must book at least 2 hours before departure)
        if (schedule.getDepartureTime().isBefore(now.plusHours(MIN_BOOKING_HOURS_BEFORE_DEPARTURE))) {
            throw new IllegalArgumentException(
                    String.format("Booking window closed - flight departs within %d hours", MIN_BOOKING_HOURS_BEFORE_DEPARTURE));
        }
        return schedule;
    }

    private List<Passenger> processPassengers(List<BookingRequest.PassengerDTO> passengerDTOs) {
        List<Passenger> passengers = new ArrayList<>();
        for (BookingRequest.PassengerDTO dto : passengerDTOs) {
            Passenger passenger = findOrCreatePassenger(dto);
            passengers.add(passenger);
        }
        return passengers;
    }

    private Passenger findOrCreatePassenger(BookingRequest.PassengerDTO dto) {
        // Check if email is already used by different passenger
        Optional<Passenger> existingByEmail = passengerRepository.findByEmail(dto.email());
        if (existingByEmail.isPresent()) {
            throw new IllegalArgumentException(
                    "Email " + dto.email() + " is already associated with a different passenger");
        }
        return createNewPassenger(dto);
    }

    private Passenger createNewPassenger(BookingRequest.PassengerDTO dto) {
        Passenger passenger = new Passenger();
        passenger.setFirstName(dto.firstName());
        passenger.setLastName(dto.lastName());
        passenger.setEmail(dto.email());
        passenger.setPassportNumber(dto.passportNumber());
        return passengerRepository.save(passenger);
    }

    private List<Seat> validateAndLockSeats(BookingRequest request, FlightSchedule schedule, String sessionId) {
        List<String> requestedSeatNumbers = request.passengers().stream()
                .map(p -> p.selectedSeatNumber().toUpperCase())
                .collect(Collectors.toList());
        List<Seat> requestedSeats = seatRepository.findByScheduleIdAndSeatNumberIn(
                schedule.getId(), requestedSeatNumbers);
        if (requestedSeats.size() != requestedSeatNumbers.size()) {
            Set<String> foundSeats = requestedSeats.stream()
                    .map(Seat::getSeatNumber)
                    .collect(Collectors.toSet());
            Set<String> missingSeats = new HashSet<>(requestedSeatNumbers);
            missingSeats.removeAll(foundSeats);
            throw new ResourceNotFoundException("Invalid seat numbers for this flight: " + missingSeats);
        }
        // Atomically lock seats to prevent race conditions
        return lockSeatsAtomically(requestedSeats, sessionId);
    }

    private List<Seat> lockSeatsAtomically(List<Seat> seats, String sessionId) {
        List<Seat> lockedSeats = new ArrayList<>();
        try {
            for (Seat seat : seats) {
                // Use SELECT FOR UPDATE to prevent race conditions
                Seat currentSeat = seatRepository.findByIdForUpdate(seat.getId())
                        .orElseThrow(() -> new ResourceNotFoundException("Seat not found: " + seat.getSeatNumber()));
                if (currentSeat.getStatus() != Seat.SeatStatus.AVAILABLE) {
                    throw new SeatUnavailableException("Seat " + currentSeat.getSeatNumber() + " is not available");
                }
                // Check for active locks by other sessions
                Optional<SeatLock> existingLock = seatLockRepository.findActiveLockBySeatId(currentSeat.getId(), LocalDateTime.now());
                if (existingLock.isPresent() && !existingLock.get().getSessionId().equals(sessionId)) {
                    throw new SeatUnavailableException("Seat " + currentSeat.getSeatNumber() + " is temporarily reserved by another user");
                }
                // Lock the seat
                currentSeat.setStatus(Seat.SeatStatus.LOCKED);
                seatRepository.save(currentSeat);
                // Create/update seat lock record
                seatLockService.createOrUpdateSeatLock(currentSeat, sessionId);
                lockedSeats.add(currentSeat);
            }
            return lockedSeats;
        } catch (Exception e) {
            // Release any seats we managed to lock
            releaseSeats(lockedSeats);
            throw e;
        }
    }

    private void validateNoDuplicateBookings(List<Passenger> passengers, FlightSchedule schedule) {
        for (Passenger passenger : passengers) {
            Optional<Booking> existingBooking = bookingRepository.findByPassengerAndSchedule(
                    passenger.getId(), schedule.getId(), Booking.BookingStatus.CONFIRMED);
            if (existingBooking.isPresent()) {
                throw new BookingConflictException(
                        "Passenger with email " + passenger.getEmail() +
                                " already has a confirmed booking on this flight");
            }
        }
    }

    private void validateFlightCapacity(FlightSchedule schedule, int requestedSeats) {
        long confirmedBookings = bookingRepository.countConfirmedBookingsBySchedule(schedule.getId(), Booking.BookingStatus.CONFIRMED);

        if (confirmedBookings + requestedSeats > schedule.getTotalSeats()) {
            throw new SeatUnavailableException(
                    "Flight is full. Available seats: " + (schedule.getTotalSeats() - confirmedBookings) +
                            ", Requested: " + requestedSeats);
        }
    }

    @Transactional(propagation = Propagation.MANDATORY)
    private Booking createBookingForPassengers(List<Passenger> passengers, List<Seat> seats,
                                               FlightSchedule schedule, BookingRequest request) {
        if (passengers.size() != seats.size()) {
            throw new IllegalStateException("Passenger count must match seat count");
        }
        try {
            Booking booking = new Booking();
            booking.setBookingReference(bookingIdGenerator.generateBookingReference());
            booking.setSchedule(schedule);
            booking.setBookingTime(ZonedDateTime.now());
            booking.setStatus(Booking.BookingStatus.CONFIRMED);
            booking = bookingRepository.save(booking);
            log.debug("Created main booking {} for {} passengers",
                    booking.getBookingReference(), passengers.size());
            List<BookingItem> bookingItems = new ArrayList<>();
            for (int i = 0; i < passengers.size(); i++) {
                BookingItem item = createBookingItem(booking, passengers.get(i), seats.get(i), request.passengers().get(i));
                bookingItems.add(item);
            }
            booking.setBookingItems(bookingItems);
            log.info("Successfully created booking {} with {} passengers",
                    booking.getBookingReference(), bookingItems.size());
            return booking;
        } catch (DataIntegrityViolationException e) {
            log.error("Data integrity violation while creating booking", e);
            throw new BookingConflictException(
                    "Booking conflict detected - one or more passengers may already be booked on this flight");
        } catch (Exception e) {
            log.error("Unexpected error while creating booking", e);
            throw new BookingConflictException("Failed to create booking: " + e.getMessage());
        }
    }

    @Transactional(propagation = Propagation.MANDATORY)
    private BookingItem createBookingItem(Booking booking, Passenger passenger, Seat seat,
                                          BookingRequest.PassengerDTO passengerDTO) {
        try {
            BookingItem item = new BookingItem();
            item.setBooking(booking);
            item.setPassenger(passenger);
            item.setSeat(seat);
            BookingItem savedItem = bookingItemRepository.save(item);
            log.debug("Created booking item for passenger {} on seat {}",
                    passenger.getPassportNumber(), seat.getSeatNumber());
            return savedItem;
        } catch (DataIntegrityViolationException e) {
            log.error("Data integrity violation while creating booking item for passenger {}",
                    passenger.getPassportNumber(), e);
            throw new BookingConflictException(
                    String.format("Booking conflict for passenger %s %s - may already be booked on this flight",
                            passenger.getFirstName(), passenger.getLastName()));
        }
    }

    @Transactional(propagation = Propagation.MANDATORY)
    private Payment createPaymentRecord(Booking booking, BookingRequest.PaymentInfoDTO paymentDTO) {
        Payment payment = new Payment();
        payment.setBooking(booking);
        payment.setMethod(Payment.PaymentMethod.valueOf(paymentDTO.method().name()));
        payment.setTotalAmountPaid(paymentDTO.price().amountPaid());
        payment.setCurrency(paymentDTO.price().currency());
        payment.setTransactionId(paymentDTO.transactionId());
        payment.setStatus(Payment.PaymentStatus.valueOf(paymentDTO.status().name()));
        return paymentRepository.save(payment);
    }

    @Transactional(propagation = Propagation.MANDATORY)
    private void confirmSeatsAndReleaseLocks(List<Seat> seats, String sessionId) {
        for (Seat seat : seats) {
            seat.setStatus(Seat.SeatStatus.BOOKED);
            seatRepository.save(seat);
        }
        // Release seat locks as booking confirmed
        List<Long> seatIds = seats.stream().map(Seat::getId).collect(Collectors.toList());
        seatLockRepository.releaseLocksForSeats(seatIds);
        log.debug("Confirmed {} seats and released locks for session {}", seats.size(), sessionId);
    }

    private BookingResponse buildBookingResponse(Booking booking) {
        List<BookingResponse.PassengerSeatDTO> passengerSeats = booking.getBookingItems().stream()
                .map(item -> new BookingResponse.PassengerSeatDTO(
                        item.getPassenger().getFirstName(),
                        item.getPassenger().getLastName(),
                        item.getPassenger().getEmail(),
                        item.getSeat().getSeatNumber()
                ))
                .collect(Collectors.toList());
        BookingResponse.PaymentInfoDTO paymentInfo = new BookingResponse.PaymentInfoDTO(
                booking.getPayment().getTransactionId(),
                booking.getPayment().getTotalAmountPaid(),
                booking.getPayment().getCurrency()
        );
        return new BookingResponse(
                booking.getBookingReference(),
                booking.getStatus().name(),
                booking.getSchedule().getFlight().getFlightCode(),
                booking.getSchedule().getDepartureTime(),
                passengerSeats,
                paymentInfo,
                booking.getBookingTime().toLocalDate()
        );
    }

    private void cleanupFailedBooking(BookingRequest request, String sessionId) {
        try {
            List<String> seatNumbers = request.passengers().stream()
                    .map(p -> p.selectedSeatNumber().toUpperCase())
                    .collect(Collectors.toList());
            seatLockService.releaseLocksForSession(sessionId, seatNumbers);
            log.debug("Cleaned up failed booking for session {}", sessionId);
        } catch (Exception e) {
            log.error("Failed to cleanup failed booking for session: {}", sessionId, e);
        }
    }

    private void releaseSeats(List<Seat> seats) {
        for (Seat seat : seats) {
            try {
                seat.setStatus(Seat.SeatStatus.AVAILABLE);
                seatRepository.save(seat);
            } catch (Exception e) {
                log.error("Failed to release seat: {}", seat.getSeatNumber(), e);
            }
        }
    }
}
