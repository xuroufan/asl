package com.futures.admin.security;

import com.futures.admin.entity.SysRole;
import com.futures.admin.entity.SysUser;
import com.futures.admin.service.SysUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Spring Security UserDetailsService — 从数据库加载用户
 */
@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final SysUserService userService;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        SysUser user = userService.getUserByUsername(username);
        if (user == null) {
            throw new UsernameNotFoundException("用户不存在: " + username);
        }
        if (user.getStatus() != null && user.getStatus() != 0) {
            throw new UsernameNotFoundException("用户已被禁用");
        }
        // 加载用户角色
        List<SysRole> roles = userService.getUserRoles(user.getUserId());
        List<String> roleKeys = roles.stream()
                .map(SysRole::getRoleKey)
                .collect(Collectors.toList());
        return new LoginUser(user, roleKeys);
    }
}
