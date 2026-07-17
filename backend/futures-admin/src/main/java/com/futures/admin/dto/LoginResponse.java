package com.futures.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * 管理后台登录响应 DTO
 */
@Data
@Builder
@AllArgsConstructor
public class LoginResponse {
    private String accessToken;
    private String refreshToken;
    private UserInfo user;

    @Data
    @Builder
    @AllArgsConstructor
    public static class UserInfo {
        private Long userId;
        private String username;
        private String nickname;
        private String email;
        private String avatar;
        private List<String> roles;
        private List<RouterVO> menus;
    }
}
