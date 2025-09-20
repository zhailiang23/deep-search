package com.deepsearch.service;

import com.deepsearch.entity.User;
import com.deepsearch.exception.BadRequestException;
import com.deepsearch.exception.ResourceNotFoundException;
import com.deepsearch.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 搜索安全服务 - 统一处理搜索相关的权限控制和用户认证
 * 消除各个搜索服务中的重复安全检查代码
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SearchSecurityService {

    private final UserRepository userRepository;

    /**
     * 获取当前用户ID
     */
    public Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new BadRequestException("用户未认证");
        }

        String username = authentication.getName();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("用户", "用户名", username));

        return user.getId();
    }

    /**
     * 获取当前用户ID（允许为null）
     */
    public Long getCurrentUserIdOrNull() {
        try {
            return getCurrentUserId();
        } catch (Exception e) {
            return null; // 允许匿名搜索
        }
    }

    /**
     * 检查当前用户是否为管理员
     */
    public boolean isAdmin() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            return false;
        }
        return authentication.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN"));
    }

    /**
     * 获取当前用户的空间ID（权限控制）
     */
    public String getCurrentUserSpaceId() {
        // TODO: 实现基于用户权限的空间ID获取
        // 暂时返回null表示不限制空间
        return null;
    }

    /**
     * 获取当前用户可访问的渠道列表（权限控制）
     */
    public List<String> getCurrentUserChannels() {
        // TODO: 实现基于用户权限的渠道列表获取
        // 暂时返回null表示不限制渠道
        return null;
    }

    /**
     * 检查用户是否有权限访问指定用户的数据
     */
    public boolean canAccessUserData(Long targetUserId) {
        if (isAdmin()) {
            return true;
        }

        Long currentUserId = getCurrentUserIdOrNull();
        return currentUserId != null && currentUserId.equals(targetUserId);
    }

    /**
     * 验证用户权限并抛出异常（如果无权限）
     */
    public void validateUserAccess(Long targetUserId, String operation) {
        if (!canAccessUserData(targetUserId)) {
            throw new BadRequestException("无权" + operation + "其他用户的数据");
        }
    }

    /**
     * 验证管理员权限
     */
    public void validateAdminAccess(String operation) {
        if (!isAdmin()) {
            throw new BadRequestException("无权" + operation);
        }
    }
}