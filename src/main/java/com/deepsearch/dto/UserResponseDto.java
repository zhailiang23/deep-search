package com.deepsearch.dto;

import com.deepsearch.entity.User;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户响应DTO
 */
@Data
public class UserResponseDto {

    private Long id;
    private String username;
    private String email;
    private User.Role role;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public UserResponseDto(User user) {
        this.id = user.getId();
        this.username = user.getUsername();
        this.email = user.getEmail();
        this.role = user.getRole();
        this.createdAt = user.getCreatedAt();
        this.updatedAt = user.getUpdatedAt();
    }
}