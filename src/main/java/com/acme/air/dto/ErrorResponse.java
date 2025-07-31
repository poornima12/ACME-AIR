package com.acme.air.dto;

public record ErrorResponse(
        ErrorInfo error
) {
    public record ErrorInfo(
            String code,
            String message
    ) {}
}
