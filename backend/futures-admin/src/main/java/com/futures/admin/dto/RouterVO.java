package com.futures.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * 前端路由 VO — 构建动态菜单树
 */
@Data
@Builder
@AllArgsConstructor
public class RouterVO {
    private Long id;
    private Long parentId;
    private String name;
    private String path;
    private String component;
    private String redirect;
    private Meta meta;

    @Data
    @Builder
    @AllArgsConstructor
    public static class Meta {
        private String title;       // 菜单标题
        private String icon;        // 图标
        private boolean hideMenu;   // 是否隐藏
        private List<String> roles; // 可访问角色
    }

    private List<RouterVO> children;
}
