package com.acme.air.model;

import jakarta.persistence.*;

import java.math.BigDecimal;

@Entity
public class Payment extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    private Booking booking;

    @Enumerated(EnumType.STRING)
    private PaymentMethod method;

    private BigDecimal amount;
    private String currency;
    private String transactionId;
    private PaymentStatus status;

    public enum PaymentMethod {
        CREDIT_CARD, DEBIT_CARD, PAYPAL
    }

    public enum PaymentStatus {
        SUCCESS, FAILED, PENDING
    }
}

