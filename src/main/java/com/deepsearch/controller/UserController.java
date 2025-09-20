package com.deepsearch.controller;

import com.deepsearch.dto.*;
import com.deepsearch.entity.User;
import com.deepsearch.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 用户管理控制器
 */
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "用户管理", description = "用户注册、登录、信息管理API")
public class UserController {

    private final UserService userService;

    /**
     * 用户注册
     */
    @Operation(summary = "用户注册", description = "创建新用户账户")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "注册成功"),
            @ApiResponse(responseCode = "409", description = "用户名或邮箱已存在"),
            @ApiResponse(responseCode = "400", description = "请求参数错误")
    })
    @PostMapping("/register")
    public ResponseEntity<com.deepsearch.dto.ApiResponse<UserResponseDto>> registerUser(
            @Valid @RequestBody UserRegistrationDto registrationDto) {
        log.info("用户注册请求: {}", registrationDto.getUsername());
        UserResponseDto userResponse = userService.registerUser(registrationDto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(com.deepsearch.dto.ApiResponse.success("用户注册成功", userResponse));
    }

    /**
     * 用户登录
     */
    @Operation(summary = "用户登录", description = "用户身份认证并获取JWT令牌")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "登录成功"),
            @ApiResponse(responseCode = "401", description = "用户名/邮箱或密码错误"),
            @ApiResponse(responseCode = "400", description = "请求参数错误")
    })
    @PostMapping("/login")
    public ResponseEntity<com.deepsearch.dto.ApiResponse<UserLoginResponseDto>> loginUser(
            @Valid @RequestBody UserLoginDto loginDto) {
        log.info("用户登录请求: {}", loginDto.getUsernameOrEmail());
        UserLoginResponseDto loginResponse = userService.authenticateUser(loginDto);
        return ResponseEntity.ok(com.deepsearch.dto.ApiResponse.success("登录成功", loginResponse));
    }

    /**
     * 获取当前用户信息
     */
    @Operation(summary = "获取当前用户信息", description = "获取当前认证用户的个人信息")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "获取成功"),
            @ApiResponse(responseCode = "401", description = "用户未认证")
    })
    @GetMapping("/me")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<com.deepsearch.dto.ApiResponse<UserResponseDto>> getCurrentUser() {
        UserResponseDto userResponse = userService.getCurrentUser();
        return ResponseEntity.ok(com.deepsearch.dto.ApiResponse.success(userResponse));
    }

    /**
     * 根据ID获取用户信息
     */
    @Operation(summary = "根据ID获取用户信息", description = "管理员获取指定用户的信息")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "获取成功"),
            @ApiResponse(responseCode = "404", description = "用户不存在"),
            @ApiResponse(responseCode = "403", description = "权限不足")
    })
    @GetMapping("/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<com.deepsearch.dto.ApiResponse<UserResponseDto>> getUserById(
            @Parameter(description = "用户ID") @PathVariable Long userId) {
        UserResponseDto userResponse = userService.getUserById(userId);
        return ResponseEntity.ok(com.deepsearch.dto.ApiResponse.success(userResponse));
    }

    /**
     * 获取所有用户列表
     */
    @Operation(summary = "获取所有用户列表", description = "管理员获取所有用户列表")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "获取成功"),
            @ApiResponse(responseCode = "403", description = "权限不足")
    })
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<com.deepsearch.dto.ApiResponse<List<UserResponseDto>>> getAllUsers() {
        List<UserResponseDto> users = userService.getAllUsers();
        return ResponseEntity.ok(com.deepsearch.dto.ApiResponse.success(users));
    }

    /**
     * 更新用户角色
     */
    @Operation(summary = "更新用户角色", description = "管理员更新用户的角色权限")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "更新成功"),
            @ApiResponse(responseCode = "404", description = "用户不存在"),
            @ApiResponse(responseCode = "403", description = "权限不足")
    })
    @PutMapping("/{userId}/role")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<com.deepsearch.dto.ApiResponse<UserResponseDto>> updateUserRole(
            @Parameter(description = "用户ID") @PathVariable Long userId,
            @Parameter(description = "新角色") @RequestParam User.Role role) {
        UserResponseDto userResponse = userService.updateUserRole(userId, role);
        return ResponseEntity.ok(com.deepsearch.dto.ApiResponse.success("用户角色更新成功", userResponse));
    }

    /**
     * 更新用户信息
     */
    @Operation(summary = "更新用户信息", description = "更新当前用户的个人信息")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "更新成功"),
            @ApiResponse(responseCode = "409", description = "邮箱已被使用"),
            @ApiResponse(responseCode = "401", description = "用户未认证")
    })
    @PutMapping("/me")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<com.deepsearch.dto.ApiResponse<UserResponseDto>> updateCurrentUser(
            @Parameter(description = "新邮箱地址") @RequestParam String email) {
        UserUpdateDto updateDto = new UserUpdateDto();
        updateDto.setEmail(email);
        UserResponseDto userResponse = userService.updateUserProfile(updateDto);
        return ResponseEntity.ok(com.deepsearch.dto.ApiResponse.success("用户信息更新成功", userResponse));
    }

    /**
     * 删除用户
     */
    @Operation(summary = "删除用户", description = "管理员删除指定用户")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "删除成功"),
            @ApiResponse(responseCode = "404", description = "用户不存在"),
            @ApiResponse(responseCode = "403", description = "权限不足")
    })
    @DeleteMapping("/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<com.deepsearch.dto.ApiResponse<String>> deleteUser(
            @Parameter(description = "用户ID") @PathVariable Long userId) {
        userService.deleteUser(userId);
        return ResponseEntity.ok(com.deepsearch.dto.ApiResponse.success("用户删除成功"));
    }

    /**
     * 检查用户名是否可用
     */
    @Operation(summary = "检查用户名可用性", description = "检查指定用户名是否已被使用")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "检查完成")
    })
    @GetMapping("/check-username")
    public ResponseEntity<com.deepsearch.dto.ApiResponse<Boolean>> checkUsernameAvailability(
            @Parameter(description = "要检查的用户名") @RequestParam String username) {
        boolean available = userService.isUsernameAvailable(username);
        return ResponseEntity.ok(com.deepsearch.dto.ApiResponse.success(
                available ? "用户名可用" : "用户名已被使用", available));
    }

    /**
     * 检查邮箱是否可用
     */
    @Operation(summary = "检查邮箱可用性", description = "检查指定邮箱是否已被使用")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "检查完成")
    })
    @GetMapping("/check-email")
    public ResponseEntity<com.deepsearch.dto.ApiResponse<Boolean>> checkEmailAvailability(
            @Parameter(description = "要检查的邮箱") @RequestParam String email) {
        boolean available = userService.isEmailAvailable(email);
        return ResponseEntity.ok(com.deepsearch.dto.ApiResponse.success(
                available ? "邮箱可用" : "邮箱已被使用", available));
    }

    /**
     * 更新用户个人资料
     */
    @Operation(summary = "更新用户个人资料", description = "更新用户的个人资料信息")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "更新成功"),
            @ApiResponse(responseCode = "409", description = "邮箱已被使用"),
            @ApiResponse(responseCode = "401", description = "用户未认证")
    })
    @PutMapping("/profile")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<com.deepsearch.dto.ApiResponse<UserResponseDto>> updateUserProfile(
            @Valid @RequestBody UserUpdateDto updateDto) {
        UserResponseDto userResponse = userService.updateUserProfile(updateDto);
        return ResponseEntity.ok(com.deepsearch.dto.ApiResponse.success("个人资料更新成功", userResponse));
    }
}
