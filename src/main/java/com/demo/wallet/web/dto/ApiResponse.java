package com.demo.wallet.web.dto;

public record ApiResponse<T>(boolean success, T data, ApiError error) {
    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(true, data, null);
    }
    public static ApiResponse<Object> fail(String code, String message) {
        return new ApiResponse<>(false, null, new ApiError(code, message));
    }
}