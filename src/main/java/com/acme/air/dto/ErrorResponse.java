package com.acme.air.dto;

public record ErrorResponse(String status, ErrorDetail error) {
    public ErrorResponse(String code, String message) {
        this("ERROR", new ErrorDetail(code, message));
    }

    public record ErrorDetail(String code, String message) {}
}
