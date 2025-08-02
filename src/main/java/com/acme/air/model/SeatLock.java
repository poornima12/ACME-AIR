package com.acme.air.model;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
public class SeatLock extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String sessionId; // Track user session

    @OneToOne
    private Seat seat;

    @ManyToOne
    private Passenger passenger;

    private LocalDateTime lockedAt;
    private LocalDateTime expiresAt; // Short-lived locks (e.g., 10 minutes)

    @Enumerated(EnumType.STRING)
    private LockStatus status;

    public enum LockStatus {
        ACTIVE, EXPIRED, CONFIRMED, RELEASED
    }
}

