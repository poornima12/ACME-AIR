package com.acme.air.model;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
public class SeatLock extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    private Seat seat;

    @ManyToOne
    private Passenger passenger;

    private LocalDateTime lockedAt;
    private LocalDateTime expiresAt;

    @Enumerated(EnumType.STRING)
    private LockStatus status;

    public enum LockStatus {
        ACTIVE, EXPIRED, CONFIRMED
    }
}

