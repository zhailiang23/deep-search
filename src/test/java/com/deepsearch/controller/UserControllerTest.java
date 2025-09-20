package com.deepsearch.controller;

import com.deepsearch.dto.UserRegistrationDto;
import com.deepsearch.dto.UserLoginDto;
import com.deepsearch.dto.UserLoginResponseDto;
import com.deepsearch.dto.UserResponseDto;
import com.deepsearch.dto.UserUpdateDto;
import com.deepsearch.entity.User;
import com.deepsearch.exception.BadRequestException;
import com.deepsearch.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 用户控制器集成测试
 */
@WebMvcTest(UserController.class)
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserService userService;

    @Autowired
    private ObjectMapper objectMapper;

    private UserRegistrationDto registrationDto;
    private UserLoginDto loginDto;
    private UserResponseDto userResponseDto;

    @BeforeEach
    void setUp() {
        registrationDto = new UserRegistrationDto();
        registrationDto.setUsername("testuser");
        registrationDto.setEmail("test@example.com");
        registrationDto.setPassword("password123");

        loginDto = new UserLoginDto();
        loginDto.setUsernameOrEmail("testuser");
        loginDto.setPassword("password123");

        userResponseDto = new UserResponseDto();
        userResponseDto.setId(1L);
        userResponseDto.setUsername("testuser");
        userResponseDto.setEmail("test@example.com");
        userResponseDto.setRole(User.Role.USER);
        userResponseDto.setCreatedAt(LocalDateTime.now());
        userResponseDto.setUpdatedAt(LocalDateTime.now());
    }

    @Test
    void registerUser_Success() throws Exception {
        // Given
        when(userService.registerUser(any(UserRegistrationDto.class))).thenReturn(userResponseDto);

        // When & Then
        mockMvc.perform(post("/api/users/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registrationDto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("用户注册成功"))
                .andExpect(jsonPath("$.data.username").value("testuser"));

        verify(userService).registerUser(any(UserRegistrationDto.class));
    }

    @Test
    void registerUser_DuplicateUsername() throws Exception {
        // Given
        when(userService.registerUser(any(UserRegistrationDto.class)))
                .thenThrow(new BadRequestException("用户名已存在"));

        // When & Then
        mockMvc.perform(post("/api/users/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registrationDto)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void registerUser_InvalidInput_BadRequest() throws Exception {
        // Given
        UserRegistrationDto invalidDto = new UserRegistrationDto();
        invalidDto.setUsername(""); // Invalid: empty username
        invalidDto.setEmail("invalid-email"); // Invalid: invalid email format
        invalidDto.setPassword("123"); // Invalid: too short

        // When & Then
        mockMvc.perform(post("/api/users/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidDto)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void loginUser_Success() throws Exception {
        // Given
        String token = "jwt-token-123";
        UserLoginResponseDto loginResponse = new UserLoginResponseDto();
        loginResponse.setToken(token);
        loginResponse.setUser(userResponseDto);

        when(userService.authenticateUser(any(UserLoginDto.class))).thenReturn(loginResponse);

        // When & Then
        mockMvc.perform(post("/api/users/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.token").value(token));

        verify(userService).authenticateUser(any(UserLoginDto.class));
    }

    @Test
    void loginUser_InvalidCredentials() throws Exception {
        // Given
        when(userService.authenticateUser(any(UserLoginDto.class)))
                .thenThrow(new BadRequestException("用户名或密码错误"));

        // When & Then
        mockMvc.perform(post("/api/users/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginDto)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void loginUser_InvalidInput_BadRequest() throws Exception {
        // Given
        UserLoginDto invalidDto = new UserLoginDto();
        invalidDto.setUsernameOrEmail(""); // Invalid: empty
        invalidDto.setPassword(""); // Invalid: empty

        // When & Then
        mockMvc.perform(post("/api/users/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidDto)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "USER")
    void getCurrentUser_Success() throws Exception {
        // Given
        when(userService.getCurrentUser()).thenReturn(userResponseDto);

        // When & Then
        mockMvc.perform(get("/api/users/me")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.username").value("testuser"));

        verify(userService).getCurrentUser();
    }

    @Test
    void getCurrentUser_Unauthorized() throws Exception {
        // Given
        when(userService.getCurrentUser())
                .thenThrow(new BadRequestException("用户未认证"));

        // When & Then
        mockMvc.perform(get("/api/users/me")
                        .with(csrf()))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void getUserById_Success() throws Exception {
        // Given
        Long userId = 1L;
        when(userService.getUserById(userId)).thenReturn(userResponseDto);

        // When & Then
        mockMvc.perform(get("/api/users/{userId}", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.username").value("testuser"));
    }

    @Test
    @WithMockUser(roles = "USER")
    void getUserById_Forbidden() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/users/{userId}", 1L))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void getAllUsers_Success() throws Exception {
        // Given
        UserResponseDto user2 = new UserResponseDto();
        user2.setId(2L);
        user2.setUsername("user2");
        user2.setEmail("user2@example.com");
        user2.setRole(User.Role.USER);

        List<UserResponseDto> users = Arrays.asList(userResponseDto, user2);
        when(userService.getAllUsers()).thenReturn(users);

        // When & Then
        mockMvc.perform(get("/api/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].username").value("testuser"))
                .andExpect(jsonPath("$.data[1].username").value("user2"));
    }

    @Test
    @WithMockUser(roles = "USER")
    void getAllUsers_Forbidden() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/users"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void updateUserRole_Success() throws Exception {
        // Given
        Long userId = 1L;
        User.Role newRole = User.Role.ADMIN;
        userResponseDto.setRole(newRole);
        when(userService.updateUserRole(userId, newRole)).thenReturn(userResponseDto);

        // When & Then
        mockMvc.perform(put("/api/users/{userId}/role", userId)
                        .with(csrf())
                        .param("role", newRole.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("用户角色更新成功"))
                .andExpect(jsonPath("$.data.role").value("ADMIN"));
    }

    @Test
    @WithMockUser(roles = "USER")
    void updateUserRole_Forbidden() throws Exception {
        // When & Then
        mockMvc.perform(put("/api/users/{userId}/role", 1L)
                        .with(csrf())
                        .param("role", "ADMIN"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "USER")
    void updateCurrentUser_Success() throws Exception {
        // Given
        String newEmail = "newemail@example.com";
        userResponseDto.setEmail(newEmail);
        when(userService.getCurrentUser()).thenReturn(userResponseDto);
        when(userService.updateUser(eq(1L), eq(newEmail))).thenReturn(userResponseDto);

        // When & Then
        mockMvc.perform(put("/api/users/me")
                        .with(csrf())
                        .param("email", newEmail))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("用户信息更新成功"))
                .andExpect(jsonPath("$.data.email").value(newEmail));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void deleteUser_Success() throws Exception {
        // Given
        Long userId = 1L;

        // When & Then
        mockMvc.perform(delete("/api/users/{userId}", userId)
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("用户删除成功"));
    }

    @Test
    @WithMockUser(roles = "USER")
    void deleteUser_Forbidden() throws Exception {
        // When & Then
        mockMvc.perform(delete("/api/users/{userId}", 1L)
                        .with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    void checkUsernameAvailability_Available() throws Exception {
        // Given
        String username = "newuser";
        when(userService.isUsernameAvailable(username)).thenReturn(true);

        // When & Then
        mockMvc.perform(get("/api/users/check-username")
                        .param("username", username))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("用户名可用"))
                .andExpect(jsonPath("$.data").value(true));
    }

    @Test
    void checkUsernameAvailability_NotAvailable() throws Exception {
        // Given
        String username = "existinguser";
        when(userService.isUsernameAvailable(username)).thenReturn(false);

        // When & Then
        mockMvc.perform(get("/api/users/check-username")
                        .param("username", username))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("用户名已被使用"))
                .andExpect(jsonPath("$.data").value(false));
    }

    @Test
    void checkEmailAvailability_Available() throws Exception {
        // Given
        String email = "new@example.com";
        when(userService.isEmailAvailable(email)).thenReturn(true);

        // When & Then
        mockMvc.perform(get("/api/users/check-email")
                        .param("email", email))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("邮箱可用"))
                .andExpect(jsonPath("$.data").value(true));
    }

    @Test
    void checkEmailAvailability_NotAvailable() throws Exception {
        // Given
        String email = "existing@example.com";
        when(userService.isEmailAvailable(email)).thenReturn(false);

        // When & Then
        mockMvc.perform(get("/api/users/check-email")
                        .param("email", email))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("邮箱已被使用"))
                .andExpect(jsonPath("$.data").value(false));
    }

    @Test
    void updateUserProfile_Success() throws Exception {
        // Given
        UserUpdateDto updateDto = new UserUpdateDto();
        updateDto.setEmail("newemail@example.com");

        UserResponseDto updatedUser = new UserResponseDto();
        updatedUser.setId(1L);
        updatedUser.setUsername("testuser");
        updatedUser.setEmail("newemail@example.com");
        updatedUser.setRole(User.Role.USER);
        updatedUser.setCreatedAt(LocalDateTime.now());
        updatedUser.setUpdatedAt(LocalDateTime.now());

        when(userService.updateUserProfile(any(UserUpdateDto.class))).thenReturn(updatedUser);

        // When & Then
        mockMvc.perform(put("/api/users/profile")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.email").value("newemail@example.com"));

        verify(userService).updateUserProfile(any(UserUpdateDto.class));
    }

    @Test
    void updateUserProfile_ValidationError() throws Exception {
        // Given
        UserUpdateDto updateDto = new UserUpdateDto();
        updateDto.setEmail("invalid-email");

        // When & Then
        mockMvc.perform(put("/api/users/profile")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDto)))
                .andExpect(status().isBadRequest());
    }
}