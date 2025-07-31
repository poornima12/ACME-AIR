package com.acme.air.dto;

import java.math.BigDecimal;

public record PaymentInfoDTO(
        PaymentMethod method,
        String transactionId,
        PriceDTO price,
        PaymentStatus status
) { }
