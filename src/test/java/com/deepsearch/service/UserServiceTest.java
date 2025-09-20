package com.deepsearch.service;

import com.deepsearch.dto.UserRegistrationDto;
import com.deepsearch.dto.UserLoginDto;
import com.deepsearch.dto.UserUpdateDto;

import com.deepsearch.dto.UserLoginResponseDto;

import com.deepsearch.dto.UserResponseDto;
import com.deepsearch.entity.User;
import com.deepsearch.exception.ConflictException;
import com.deepsearch.exception.ResourceNotFoundException;
import com.deepsearch.repository.UserRepository;
import com.deepsearch.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 用户服务单元测试
 */
@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @InjectMocks
    private UserService userService;

    private User testUser;
    private UserRegistrationDto registrationDto;
    private UserLoginDto loginDto;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");
        testUser.setEmail("test@example.com");
        testUser.setPasswordHash("hashedPassword");
        testUser.setRole(User.Role.USER);
        testUser.setCreatedAt(LocalDateTime.now());
        testUser.setUpdatedAt(LocalDateTime.now());

        registrationDto = new UserRegistrationDto();
        registrationDto.setUsername("testuser");
        registrationDto.setEmail("test@example.com");
        registrationDto.setPassword("password123");

        loginDto = new UserLoginDto();
        loginDto.setUsernameOrEmail("testuser");
        loginDto.setPassword("password123");
    }

    @Test
    void registerUser_Success() {
        // Given
        when(userRepository.existsByUsername(registrationDto.getUsername())).thenReturn(false);
        when(userRepository.existsByEmail(registrationDto.getEmail())).thenReturn(false);
        when(passwordEncoder.encode(registrationDto.getPassword())).thenReturn("hashedPassword");
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // When
        UserResponseDto result = userService.registerUser(registrationDto);

        // Then
        assertNotNull(result);
        assertEquals(testUser.getId(), result.getId());
        assertEquals(testUser.getUsername(), result.getUsername());
        assertEquals(testUser.getEmail(), result.getEmail());
        assertEquals(testUser.getRole(), result.getRole());

        verify(userRepository).existsByUsername(registrationDto.getUsername());
        verify(userRepository).existsByEmail(registrationDto.getEmail());
        verify(passwordEncoder).encode(registrationDto.getPassword());
        verify(userRepository).save(any(User.class));
    }

    @Test
    void registerUser_UsernameExists_ThrowsConflictException() {
        // Given
        when(userRepository.existsByUsername(registrationDto.getUsername())).thenReturn(true);

        // When & Then
        ConflictException exception = assertThrows(ConflictException.class,
                () -> userService.registerUser(registrationDto));

        assertTrue(exception.getMessage().contains("用户名已存在"));
        verify(userRepository).existsByUsername(registrationDto.getUsername());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void registerUser_EmailExists_ThrowsConflictException() {
        // Given
        when(userRepository.existsByUsername(registrationDto.getUsername())).thenReturn(false);
        when(userRepository.existsByEmail(registrationDto.getEmail())).thenReturn(true);

        // When & Then
        ConflictException exception = assertThrows(ConflictException.class,
                () -> userService.registerUser(registrationDto));

        assertTrue(exception.getMessage().contains("邮箱已存在"));
        verify(userRepository).existsByUsername(registrationDto.getUsername());
        verify(userRepository).existsByEmail(registrationDto.getEmail());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void authenticateUser_Success() {
        // Given
        Authentication mockAuthentication = mock(Authentication.class);
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(mockAuthentication);
        when(jwtTokenProvider.generateToken(mockAuthentication)).thenReturn("jwt-token");

        // When
        UserLoginResponseDto result = userService.authenticateUser(loginDto);

        // Then
        assertEquals("jwt-token", result.getToken());
        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
        verify(jwtTokenProvider).generateToken(mockAuthentication);
    }

    @Test
    void getUserById_Success() {
        // Given
        when(userRepository.findById(testUser.getId())).thenReturn(Optional.of(testUser));

        // When
        UserResponseDto result = userService.getUserById(testUser.getId());

        // Then
        assertNotNull(result);
        assertEquals(testUser.getId(), result.getId());
        assertEquals(testUser.getUsername(), result.getUsername());
        verify(userRepository).findById(testUser.getId());
    }

    @Test
    void getUserById_NotFound_ThrowsResourceNotFoundException() {
        // Given
        Long userId = 999L;
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        // When & Then
        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class,
                () -> userService.getUserById(userId));

        assertTrue(exception.getMessage().contains("用户"));
        verify(userRepository).findById(userId);
    }

    @Test
    void getUserByUsername_Success() {
        // Given
        when(userRepository.findByUsername(testUser.getUsername())).thenReturn(Optional.of(testUser));

        // When
        UserResponseDto result = userService.getUserByUsername(testUser.getUsername());

        // Then
        assertNotNull(result);
        assertEquals(testUser.getUsername(), result.getUsername());
        verify(userRepository).findByUsername(testUser.getUsername());
    }

    @Test
    void getUserByEmail_Success() {
        // Given
        when(userRepository.findByEmail(testUser.getEmail())).thenReturn(Optional.of(testUser));

        // When
        UserResponseDto result = userService.getUserByEmail(testUser.getEmail());

        // Then
        assertNotNull(result);
        assertEquals(testUser.getEmail(), result.getEmail());
        verify(userRepository).findByEmail(testUser.getEmail());
    }

    @Test
    void getAllUsers_Success() {
        // Given
        User user2 = new User();
        user2.setId(2L);
        user2.setUsername("user2");
        user2.setEmail("user2@example.com");
        user2.setRole(User.Role.USER);

        List<User> users = Arrays.asList(testUser, user2);
        when(userRepository.findAll()).thenReturn(users);

        // When
        List<UserResponseDto> result = userService.getAllUsers();

        // Then
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals(testUser.getUsername(), result.get(0).getUsername());
        assertEquals(user2.getUsername(), result.get(1).getUsername());
        verify(userRepository).findAll();
    }

    @Test
    void updateUserRole_Success() {
        // Given
        when(userRepository.findById(testUser.getId())).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // When
        UserResponseDto result = userService.updateUserRole(testUser.getId(), User.Role.ADMIN);

        // Then
        assertNotNull(result);
        verify(userRepository).findById(testUser.getId());
        verify(userRepository).save(testUser);
    }

    @Test
    void deleteUser_Success() {
        // Given
        when(userRepository.findById(testUser.getId())).thenReturn(Optional.of(testUser));

        // When
        userService.deleteUser(testUser.getId());

        // Then
        verify(userRepository).findById(testUser.getId());
        verify(userRepository).delete(testUser);
    }

    @Test
    void isUsernameAvailable_Available() {
        // Given
        when(userRepository.existsByUsername("newuser")).thenReturn(false);

        // When
        boolean result = userService.isUsernameAvailable("newuser");

        // Then
        assertTrue(result);
        verify(userRepository).existsByUsername("newuser");
    }

    @Test
    void isUsernameAvailable_NotAvailable() {
        // Given
        when(userRepository.existsByUsername("existinguser")).thenReturn(true);

        // When
        boolean result = userService.isUsernameAvailable("existinguser");

        // Then
        assertFalse(result);
        verify(userRepository).existsByUsername("existinguser");
    }

    @Test
    void isEmailAvailable_Available() {
        // Given
        when(userRepository.existsByEmail("new@example.com")).thenReturn(false);

        // When
        boolean result = userService.isEmailAvailable("new@example.com");

        // Then
        assertTrue(result);
        verify(userRepository).existsByEmail("new@example.com");
    }

    @Test
    void isEmailAvailable_NotAvailable() {
        // Given
        when(userRepository.existsByEmail("existing@example.com")).thenReturn(true);

        // When
        boolean result = userService.isEmailAvailable("existing@example.com");

        // Then
        assertFalse(result);
        verify(userRepository).existsByEmail("existing@example.com");
    }

    @Test
    void updateUser_Success() {
        // Given
        String newEmail = "newemail@example.com";
        when(userRepository.findById(testUser.getId())).thenReturn(Optional.of(testUser));
        when(userRepository.existsByEmail(newEmail)).thenReturn(false);
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // When
        UserResponseDto result = userService.updateUserProfile(createUpdateDto(newEmail));

        // Then
        assertNotNull(result);
        verify(userRepository).findById(testUser.getId());
        verify(userRepository).existsByEmail(newEmail);
        verify(userRepository).save(testUser);
    }

    @Test
    void updateUser_EmailConflict_ThrowsConflictException() {
        // Given
        String conflictEmail = "conflict@example.com";
        when(userRepository.findById(testUser.getId())).thenReturn(Optional.of(testUser));
        when(userRepository.existsByEmail(conflictEmail)).thenReturn(true);

        // When & Then
        ConflictException exception = assertThrows(ConflictException.class,
                () -> userService.updateUserProfile(createUpdateDto(conflictEmail)));

        assertTrue(exception.getMessage().contains("邮箱已被其他用户使用"));
        verify(userRepository).findById(testUser.getId());
        verify(userRepository).existsByEmail(conflictEmail);
        verify(userRepository, never()).save(any(User.class));
    }

    private UserUpdateDto createUpdateDto(String email) {
        UserUpdateDto updateDto = new UserUpdateDto();
        updateDto.setEmail(email);
        return updateDto;
    }
}