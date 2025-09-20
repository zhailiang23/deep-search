package com.deepsearch.service;

import com.deepsearch.dto.UserRegistrationDto;
import com.deepsearch.dto.UserLoginDto;
import com.deepsearch.dto.UserResponseDto;
import com.deepsearch.entity.User;
import com.deepsearch.exception.ConflictException;
import com.deepsearch.exception.ResourceNotFoundException;
import com.deepsearch.dto.UserLoginResponseDto;
import com.deepsearch.dto.UserUpdateDto;
import com.deepsearch.exception.BadRequestException;
import com.deepsearch.repository.UserRepository;
import com.deepsearch.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 用户业务服务
 */
@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;

    /**
     * 用户注册
     */
    public UserResponseDto registerUser(UserRegistrationDto registrationDto) {
        // 检查用户名是否已存在
        if (userRepository.existsByUsername(registrationDto.getUsername())) {
            throw new ConflictException("用户名已存在: " + registrationDto.getUsername());
        }

        // 检查邮箱是否已存在
        if (userRepository.existsByEmail(registrationDto.getEmail())) {
            throw new ConflictException("邮箱已存在: " + registrationDto.getEmail());
        }

        // 创建新用户
        User user = new User();
        user.setUsername(registrationDto.getUsername());
        user.setEmail(registrationDto.getEmail());
        user.setPasswordHash(passwordEncoder.encode(registrationDto.getPassword()));
        user.setRole(User.Role.USER);

        User savedUser = userRepository.save(user);
        log.info("用户注册成功: {}", savedUser.getUsername());

        return new UserResponseDto(savedUser);
    }

    /**
     * 用户登录
     */
    public UserLoginResponseDto authenticateUser(UserLoginDto loginDto) {
        // 认证用户
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        loginDto.getUsernameOrEmail(),
                        loginDto.getPassword()
                )
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);

        // 生成JWT令牌
        String jwt = jwtTokenProvider.generateToken(authentication);
        log.info("用户登录成功: {}", loginDto.getUsernameOrEmail());

        UserResponseDto userDto = getCurrentUser();
        return new UserLoginResponseDto(jwt, userDto);
    }

    /**
     * 根据ID获取用户信息
     */
    @Transactional(readOnly = true)
    public UserResponseDto getUserById(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("用户", "id", userId));
        return new UserResponseDto(user);
    }

    /**
     * 根据用户名获取用户信息
     */
    @Transactional(readOnly = true)
    public UserResponseDto getUserByUsername(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("用户", "用户名", username));
        return new UserResponseDto(user);
    }

    /**
     * 根据邮箱获取用户信息
     */
    @Transactional(readOnly = true)
    public UserResponseDto getUserByEmail(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("用户", "邮箱", email));
        return new UserResponseDto(user);
    }

    /**
     * 获取所有用户列表（仅管理员）
     */
    @Transactional(readOnly = true)
    public List<UserResponseDto> getAllUsers() {
        return userRepository.findAll().stream()
                .map(UserResponseDto::new)
                .collect(Collectors.toList());
    }

    /**
     * 更新用户角色（仅管理员）
     */
    public UserResponseDto updateUserRole(Long userId, User.Role newRole) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("用户", "id", userId));

        user.setRole(newRole);
        User updatedUser = userRepository.save(user);
        log.info("用户角色更新成功: {} -> {}", user.getUsername(), newRole);

        return new UserResponseDto(updatedUser);
    }

    /**
     * 更新用户个人资料
     */
    public UserResponseDto updateUserProfile(UserUpdateDto updateDto) {
        UserResponseDto currentUser = getCurrentUser();
        Long userId = currentUser.getId();
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("用户", "id", userId));

        // 更新邮箱
        if (updateDto.getEmail() != null && !user.getEmail().equals(updateDto.getEmail())) {
            if (userRepository.existsByEmail(updateDto.getEmail())) {
                throw new ConflictException("邮箱已被其他用户使用: " + updateDto.getEmail());
            }
            user.setEmail(updateDto.getEmail());
        }

        // 更新密码
        if (updateDto.getPassword() != null) {
            user.setPasswordHash(passwordEncoder.encode(updateDto.getPassword()));
        }

        User updatedUser = userRepository.save(user);
        log.info("用户个人资料更新成功: {}", user.getUsername());

        return new UserResponseDto(updatedUser);
    }

    /**
     * 删除用户（仅管理员）
     */
    public void deleteUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("用户", "id", userId));

        userRepository.delete(user);
        log.info("用户删除成功: {}", user.getUsername());
    }

    /**
     * 检查用户名是否可用
     */
    @Transactional(readOnly = true)
    public boolean isUsernameAvailable(String username) {
        return !userRepository.existsByUsername(username);
    }

    /**
     * 检查邮箱是否可用
     */
    @Transactional(readOnly = true)
    public boolean isEmailAvailable(String email) {
        return !userRepository.existsByEmail(email);
    }

    /**
     * 获取当前认证用户
     */
    @Transactional(readOnly = true)
    public UserResponseDto getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new BadRequestException("用户未认证");
        }

        String username = authentication.getName();
        return getUserByUsername(username);
    }
}
