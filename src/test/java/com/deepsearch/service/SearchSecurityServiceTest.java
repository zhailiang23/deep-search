package com.deepsearch.service;

import com.deepsearch.entity.User;
import com.deepsearch.exception.BadRequestException;
import com.deepsearch.exception.ResourceNotFoundException;
import com.deepsearch.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Collection;
import java.util.Collections;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

/**
 * 搜索安全服务测试
 */
@ExtendWith(MockitoExtension.class)
@SuppressWarnings({"unchecked", "rawtypes", "cast"})
class SearchSecurityServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private SecurityContext securityContext;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private SearchSecurityService searchSecurityService;

    private User testUser;

    @BeforeEach
    void setUp() {
        // 创建测试用户
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");
        testUser.setEmail("test@example.com");

        // 设置SecurityContext
        SecurityContextHolder.setContext(securityContext);
    }

    @Test
    void testGetCurrentUserId_Success() {
        // 准备mock
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getName()).thenReturn("testuser");
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));

        // 执行测试
        Long result = searchSecurityService.getCurrentUserId();

        // 验证结果
        assertThat(result).isEqualTo(1L);
        verify(userRepository).findByUsername("testuser");
    }

    @Test
    void testGetCurrentUserId_NotAuthenticated() {
        // 准备mock - 未认证
        when(securityContext.getAuthentication()).thenReturn(null);

        // 执行测试并验证异常
        assertThatThrownBy(() -> searchSecurityService.getCurrentUserId())
            .isInstanceOf(BadRequestException.class)
            .hasMessage("用户未认证");
    }

    @Test
    void testGetCurrentUserId_AuthenticationNotAuthenticated() {
        // 准备mock - 认证对象存在但未认证
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(false);

        // 执行测试并验证异常
        assertThatThrownBy(() -> searchSecurityService.getCurrentUserId())
            .isInstanceOf(BadRequestException.class)
            .hasMessage("用户未认证");
    }

    @Test
    void testGetCurrentUserId_UserNotFound() {
        // 准备mock - 用户不存在
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getName()).thenReturn("nonexistent");
        when(userRepository.findByUsername("nonexistent")).thenReturn(Optional.empty());

        // 执行测试并验证异常
        assertThatThrownBy(() -> searchSecurityService.getCurrentUserId())
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessage("用户 未找到，用户名: nonexistent");
    }

    @Test
    void testGetCurrentUserIdOrNull_Success() {
        // 准备mock
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getName()).thenReturn("testuser");
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));

        // 执行测试
        Long result = searchSecurityService.getCurrentUserIdOrNull();

        // 验证结果
        assertThat(result).isEqualTo(1L);
    }

    @Test
    void testGetCurrentUserIdOrNull_ReturnsNull() {
        // 准备mock - 未认证
        when(securityContext.getAuthentication()).thenReturn(null);

        // 执行测试
        Long result = searchSecurityService.getCurrentUserIdOrNull();

        // 验证结果
        assertThat(result).isNull();
    }

    @Test
    void testIsAdmin_True() {
        // 准备mock - 管理员权限
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getAuthorities()).thenReturn(
            (Collection) Collections.singletonList(new SimpleGrantedAuthority("ROLE_ADMIN"))
        );

        // 执行测试
        boolean result = searchSecurityService.isAdmin();

        // 验证结果
        assertThat(result).isTrue();
    }

    @Test
    void testIsAdmin_False() {
        // 准备mock - 普通用户权限
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getAuthorities()).thenReturn(
            (Collection) Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"))
        );

        // 执行测试
        boolean result = searchSecurityService.isAdmin();

        // 验证结果
        assertThat(result).isFalse();
    }

    @Test
    void testIsAdmin_NoAuthentication() {
        // 准备mock - 无认证信息
        when(securityContext.getAuthentication()).thenReturn(null);

        // 执行测试
        boolean result = searchSecurityService.isAdmin();

        // 验证结果
        assertThat(result).isFalse();
    }

    @Test
    void testGetCurrentUserSpaceId() {
        // 执行测试 - 当前实现返回null
        String result = searchSecurityService.getCurrentUserSpaceId();

        // 验证结果
        assertThat(result).isNull();
    }

    @Test
    void testGetCurrentUserChannels() {
        // 执行测试 - 当前实现返回null
        java.util.List<String> result = searchSecurityService.getCurrentUserChannels();

        // 验证结果
        assertThat(result).isNull();
    }

    @Test
    void testCanAccessUserData_Admin() {
        // 准备mock - 管理员
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getAuthorities()).thenReturn(
            (Collection) Collections.singletonList(new SimpleGrantedAuthority("ROLE_ADMIN"))
        );

        // 执行测试
        boolean result = searchSecurityService.canAccessUserData(999L);

        // 验证结果 - 管理员可以访问任何用户数据
        assertThat(result).isTrue();
    }

    @Test
    void testCanAccessUserData_SameUser() {
        // 准备mock - 同一用户
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getAuthorities()).thenReturn(
            (Collection) Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"))
        );
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getName()).thenReturn("testuser");
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));

        // 执行测试
        boolean result = searchSecurityService.canAccessUserData(1L);

        // 验证结果 - 可以访问自己的数据
        assertThat(result).isTrue();
    }

    @Test
    void testCanAccessUserData_DifferentUser() {
        // 准备mock - 不同用户
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getAuthorities()).thenReturn(
            (Collection) Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"))
        );
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getName()).thenReturn("testuser");
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));

        // 执行测试
        boolean result = searchSecurityService.canAccessUserData(999L);

        // 验证结果 - 不能访问其他用户数据
        assertThat(result).isFalse();
    }

    @Test
    void testValidateUserAccess_Success() {
        // 准备mock - 管理员
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getAuthorities()).thenReturn(
            (Collection) Collections.singletonList(new SimpleGrantedAuthority("ROLE_ADMIN"))
        );

        // 执行测试 - 应该不抛出异常
        searchSecurityService.validateUserAccess(999L, "测试操作");

        // 验证没有异常抛出
    }

    @Test
    void testValidateUserAccess_Failure() {
        // 准备mock - 普通用户访问其他用户数据
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getAuthorities()).thenReturn(
            (Collection) Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"))
        );
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getName()).thenReturn("testuser");
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));

        // 执行测试并验证异常
        assertThatThrownBy(() -> searchSecurityService.validateUserAccess(999L, "测试操作"))
            .isInstanceOf(BadRequestException.class)
            .hasMessage("无权测试操作其他用户的数据");
    }

    @Test
    void testValidateAdminAccess_Success() {
        // 准备mock - 管理员
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getAuthorities()).thenReturn(
            (Collection) Collections.singletonList(new SimpleGrantedAuthority("ROLE_ADMIN"))
        );

        // 执行测试 - 应该不抛出异常
        searchSecurityService.validateAdminAccess("测试操作");

        // 验证没有异常抛出
    }

    @Test
    void testValidateAdminAccess_Failure() {
        // 准备mock - 普通用户
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getAuthorities()).thenReturn(
            (Collection) Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"))
        );

        // 执行测试并验证异常
        assertThatThrownBy(() -> searchSecurityService.validateAdminAccess("测试操作"))
            .isInstanceOf(BadRequestException.class)
            .hasMessage("无权测试操作");
    }
}