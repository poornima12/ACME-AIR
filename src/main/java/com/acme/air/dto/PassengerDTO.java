package com.acme.air.dto;

import java.time.LocalDate;

public record PassengerDTO(
        String firstName,
        String lastName,
        String email,
        String phone,
        LocalDate dateOfBirth,
        String passportNumber
) { }
