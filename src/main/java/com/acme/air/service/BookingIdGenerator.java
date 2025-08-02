package com.acme.air.service;

import org.springframework.stereotype.Service;

import java.security.SecureRandom;

@Service
public class BookingIdGenerator {

    private static final String BOOKING_PREFIX = "AIR";
    private static final SecureRandom random = new SecureRandom();
    private static final String CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";

    public String generateBookingReference() {
        StringBuilder sb = new StringBuilder();
        sb.append(BOOKING_PREFIX);

        // Add timestamp component (last 4 digits of current time)
        long timestamp = System.currentTimeMillis();
        sb.append(String.format("%04d", timestamp % 10000));

        // Add random component
        for (int i = 0; i < 4; i++) {
            sb.append(CHARS.charAt(random.nextInt(CHARS.length())));
        }

        return sb.toString(); // e.g., AIR1234ABCD
    }
}
