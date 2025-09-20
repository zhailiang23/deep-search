package com.deepsearch.dto;

import lombok.Data;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

/**
 * 用户更新DTO
 */
@Data
public class UserUpdateDto {

    @Email(message = "邮箱格式不正确")
    private String email;

    @Size(min = 6, max = 100, message = "密码长度必须在6-100之间")
    private String password;

    private String firstName;

    private String lastName;

    public UserUpdateDto() {
    }
}