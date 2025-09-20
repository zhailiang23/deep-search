package com.deepsearch.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 统一API响应DTO
 */
@Data
public class ApiResponse<T> {

    private Boolean success;
    private String message;
    private T data;
    private LocalDateTime timestamp;

    public ApiResponse() {
        this.timestamp = LocalDateTime.now();
    }

    public ApiResponse(Boolean success, String message) {
        this();
        this.success = success;
        this.message = message;
    }

    public ApiResponse(Boolean success, String message, T data) {
        this(success, message);
        this.data = data;
    }

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(true, "操作成功", data);
    }

    public static <T> ApiResponse<T> success(String message, T data) {
        return new ApiResponse<>(true, message, data);
    }

    public static <T> ApiResponse<T> success(String message) {
        return new ApiResponse<>(true, message);
    }

    public static <T> ApiResponse<T> error(String message) {
        return new ApiResponse<>(false, message);
    }
}