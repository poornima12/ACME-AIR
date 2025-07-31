package com.acme.air.dto;

public record ApiResponse<T>(String status, T data) {
    public ApiResponse(T data) {
        this("SUCCESS", data);
    }
}

