package com.acme.air.dto;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public record PriceDTO(
        @NotNull
        @DecimalMin(value = "0.0", inclusive = false, message = "Amount must be positive")
        @Digits(integer = 10, fraction = 2, message = "Amount must have max 2 decimal places")
        BigDecimal amount,
        @NotBlank
        @Pattern(regexp = "^[A-Z]{3}$", message = "Currency must be a 3-letter ISO code (e.g. NZD)")
        String currency
) { }
