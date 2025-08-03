package com.acme.air.mapper;

import com.acme.air.generated.dto.BookingRequest;

public final class DTOMapper {

    // Private constructor to prevent instantiation
    private DTOMapper() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    public static com.acme.air.generated.dto.FlightSearchResponse convertToGeneratedDTO(
            com.acme.air.dto.FlightSearchResponse existing) {

        var generatedFlights = existing.flights().stream()
                .map(DTOMapper::convertFlightDTO)
                .toList();

        return new com.acme.air.generated.dto.FlightSearchResponse()
                .flights(generatedFlights);
    }

    private static com.acme.air.generated.dto.FlightDTO convertFlightDTO(
            com.acme.air.dto.FlightSearchResponse.FlightDTO existing) {

        return new com.acme.air.generated.dto.FlightDTO()
                .flightScheduleId(existing.flightScheduleId())
                .flightNumber(existing.flightNumber())
                .airline(existing.airline())
                .origin(existing.origin())
                .destination(existing.destination())
                .departureTime(existing.departureTime().toOffsetDateTime())
                .arrivalTime(existing.arrivalTime().toOffsetDateTime())
                .pricePerSeat(existing.pricePerSeat())
                .numberOfPassengers(existing.numberOfPassengers())
                .totalPrice(existing.totalPrice())
                .availableSeats(existing.availableSeats())
                .availableSeatNumbers(existing.availableSeatNumbers());
    }

    public static com.acme.air.dto.BookingRequest convertToExistingDTO(BookingRequest generated) {
        var passengers = generated.getPassengers().stream()
                .map(p -> new com.acme.air.dto.BookingRequest.PassengerDTO(
                        p.getFirstName(),
                        p.getLastName(),
                        p.getEmail(),
                        p.getPassportNumber(),
                        p.getSelectedSeatNumber()))
                .toList();

        var payment = new com.acme.air.dto.BookingRequest.PaymentInfoDTO(
                convertPaymentMethod(generated.getPayment().getMethod()),
                generated.getPayment().getTransactionId(),
                new com.acme.air.dto.BookingRequest.PriceDTO(
                        generated.getPayment().getPrice().getAmountPaid(),
                        generated.getPayment().getPrice().getCurrency()),
                convertPaymentStatus(generated.getPayment().getStatus()));

        return new com.acme.air.dto.BookingRequest(
                generated.getFlightScheduleId(),
                passengers,
                payment);
    }

    public static com.acme.air.generated.dto.BookingResponse convertToGeneratedBookingResponse(
            com.acme.air.dto.BookingResponse existing) {

        var passengers = existing.passengers().stream()
                .map(p -> new com.acme.air.generated.dto.PassengerSeatDTO()
                        .firstName(p.firstName())
                        .lastName(p.lastName())
                        .email(p.email())
                        .seatNumber(p.seatNumber()))
                .toList();

        var payment = new com.acme.air.generated.dto.BookingPaymentInfoDTO()
                .transactionId(existing.payment().transactionId())
                .amountPaid(existing.payment().amountPaid())
                .currency(existing.payment().currency());

        return new com.acme.air.generated.dto.BookingResponse()
                .bookingId(existing.bookingId())
                .status(existing.status())
                .flightNumber(existing.flightNumber())
                .departureDate(existing.departureDate().toLocalDate())
                .passengers(passengers)
                .payment(payment)
                .createdAt(existing.createdAt());
    }

    private static com.acme.air.dto.PaymentMethod convertPaymentMethod(
            com.acme.air.generated.dto.PaymentMethod generated) {
        return com.acme.air.dto.PaymentMethod.valueOf(generated.getValue());
    }

    private static com.acme.air.dto.PaymentStatus convertPaymentStatus(
            com.acme.air.generated.dto.PaymentStatus generated) {
        return com.acme.air.dto.PaymentStatus.valueOf(generated.getValue());
    }
}