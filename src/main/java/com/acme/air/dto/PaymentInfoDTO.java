package com.acme.air.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record PaymentInfoDTO(
        @NotNull(message = "Payment method is required")
        PaymentMethod method,
        @NotBlank(message = "Transaction ID is required")
        String transactionId,
        @NotNull(message = "Price information is required")
        @Valid
        PriceDTO price,
        @NotNull(message = "Payment status is required")
        PaymentStatus status
) { }
