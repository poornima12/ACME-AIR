package com.acme.air.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@EqualsAndHashCode(callSuper = true, exclude = {"booking"})
@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Payment extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    private Booking booking;

    @Enumerated(EnumType.STRING)
    private PaymentMethod method;

    private BigDecimal totalAmountPaid;
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

