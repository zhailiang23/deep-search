package com.deepsearch.service;

import com.deepsearch.entity.User;
import com.deepsearch.repository.UserRepository;
import com.deepsearch.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 自定义用户详情服务
 * 实现Spring Security的UserDetailsService接口
 */
@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    @Transactional
    public UserDetails loadUserByUsername(String usernameOrId) throws UsernameNotFoundException {
        // 尝试通过ID加载用户
        try {
            Long userId = Long.parseLong(usernameOrId);
            User user = userRepository.findById(userId)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with id : " + userId));
            return UserPrincipal.create(user);
        } catch (NumberFormatException e) {
            // 如果不是数字，则通过用户名加载
            User user = userRepository.findByUsername(usernameOrId)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with username : " + usernameOrId));
            return UserPrincipal.create(user);
        }
    }
}