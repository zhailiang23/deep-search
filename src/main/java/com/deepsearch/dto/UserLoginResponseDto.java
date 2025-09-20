package com.deepsearch.dto;

import lombok.Data;

/**
 * 用户登录响应DTO
 */
@Data
public class UserLoginResponseDto {

    private String token;
    private UserResponseDto user;

    public UserLoginResponseDto() {
    }

    public UserLoginResponseDto(String token, UserResponseDto user) {
        this.token = token;
        this.user = user;
    }
}