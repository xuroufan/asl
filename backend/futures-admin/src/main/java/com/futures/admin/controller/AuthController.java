package com.futures.admin.controller;

import com.futures.admin.aspect.Log;
import com.futures.admin.dto.LoginRequest;
import com.futures.admin.dto.RegisterRequest;
import com.futures.admin.dto.LoginResponse;
import com.futures.admin.dto.RouterVO;
import com.futures.admin.entity.SysLoginLog;
import com.futures.admin.entity.SysMenu;
import com.futures.admin.entity.SysRole;
import com.futures.admin.entity.SysUser;
import com.futures.admin.security.JwtTokenUtil;
import com.futures.admin.security.LoginUser;
import com.futures.admin.service.SysUserService;
import com.futures.common.result.Result;
import jakarta.validation.Valid;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import com.futures.common.util.RedisUtil;
import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;
import java.util.ArrayList;
import java.util.Map;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 管理后台 — 认证 Controller
 */
@RestController
@RequestMapping("/api/v1/admin/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenUtil jwtTokenUtil;
    private final SysUserService userService;
    private final PasswordEncoder passwordEncoder;
    private final RedisUtil redisUtil;

    /** 登录 */
    @PostMapping("/login")
    @Log(title = "用户登录", operType = 5)
    public Result<LoginResponse> login(@RequestBody LoginRequest request, HttpServletRequest httpReq) {
        // 登录失败锁定检查
        String lockKey = "login:lock:" + request.getUsername();
        String lockVal = redisUtil.get(lockKey);
        if (lockVal != null) {
            long remaining = Long.parseLong(lockVal) - System.currentTimeMillis();
            if (remaining > 0) {
                return Result.error(429, "账户已锁定，请 " + (remaining / 1000) + " 秒后重试");
            }
        }

        // 记录登录日志
        SysLoginLog loginLog = new SysLoginLog();
        loginLog.setUsername(request.getUsername());
        loginLog.setIp(httpReq.getRemoteAddr());
        loginLog.setLoginTime(LocalDateTime.now());

        try {
            Authentication auth = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword()));
            LoginUser loginUser = (LoginUser) auth.getPrincipal();
            SysUser user = userService.getUserByUsername(request.getUsername());

            // 生成 Token
            List<String> roles = loginUser.getRoles();
            String accessToken = jwtTokenUtil.generateToken(user.getUserId(), user.getUsername(), roles);
            String refreshToken = jwtTokenUtil.generateRefreshToken(user.getUserId());

            // 构建菜单树
            List<SysMenu> menus = userService.getMenusByUserId(user.getUserId());
            List<RouterVO> routerTree = buildRouterTree(menus);

            // 更新登录信息
            user.setLoginIp(httpReq.getRemoteAddr());
            user.setLoginDate(LocalDateTime.now());
            userService.updateById(user);

            // 登录成功: 清除锁定计数
            redisUtil.delete(lockKey);
            redisUtil.delete("login:attempts:" + request.getUsername());

            // 登录日志
            loginLog.setStatus(0);
            loginLog.setMsg("登录成功");
            userService.saveLoginLog(loginLog);

            LoginResponse.UserInfo userInfo = LoginResponse.UserInfo.builder()
                    .userId(user.getUserId())
                    .username(user.getUsername())
                    .nickname(user.getNickname())
                    .avatar(user.getAvatar())
                    .roles(roles)
                    .menus(routerTree)
                    .build();

            return Result.success(LoginResponse.builder()
                    .accessToken(accessToken)
                    .refreshToken(refreshToken)
                    .user(userInfo)
                    .build());
        } catch (BadCredentialsException e) {
            loginLog.setStatus(1);
            loginLog.setMsg("密码错误");
            userService.saveLoginLog(loginLog);

            // 登录失败计数 + 锁定
            String attemptsKey = "login:attempts:" + request.getUsername();
            long attempts = redisUtil.increment(attemptsKey, 1);
            redisUtil.expire(attemptsKey, 900, TimeUnit.SECONDS); // 15分钟过期
            if (attempts >= 5) {
                redisUtil.set(lockKey, String.valueOf(System.currentTimeMillis() + 900000), 900, TimeUnit.SECONDS);
                return Result.error(429, "登录失败次数过多，账户已锁定 15 分钟");
            }

            return Result.error(401, "用户名或密码错误");
        }
    }

    /** 获取当前用户信息 + 路由菜单 */
    @GetMapping("/userinfo")
    public Result<LoginResponse.UserInfo> getUserInfo() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof LoginUser)) {
            return Result.unauthorized("未登录");
        }
        LoginUser loginUser = (LoginUser) auth.getPrincipal();
        SysUser user = userService.getById(loginUser.getUserId());
        List<SysMenu> menus = userService.getMenusByUserId(user.getUserId());
        List<RouterVO> routerTree = buildRouterTree(menus);

        return Result.success(LoginResponse.UserInfo.builder()
                .userId(user.getUserId())
                .username(user.getUsername())
                .nickname(user.getNickname())
                .avatar(user.getAvatar())
                .roles(loginUser.getRoles())
                .menus(routerTree)
                .build());
    }

    /** 刷新 Token */
    @PostMapping("/refresh")
    public Result<LoginResponse> refreshToken(@RequestParam String refreshToken) {
        if (!jwtTokenUtil.validateToken(refreshToken)) {
            return Result.unauthorized("Refresh Token 已过期");
        }
        Long userId = jwtTokenUtil.getUserIdFromToken(refreshToken);
        SysUser user = userService.getById(userId);
        if (user == null || user.getStatus() != 0) {
            return Result.unauthorized("用户不存在或已禁用");
        }
        List<SysRole> roles = userService.getUserRoles(userId);
        List<String> roleKeys = roles.stream().map(SysRole::getRoleKey).collect(Collectors.toList());
        String newToken = jwtTokenUtil.generateToken(userId, user.getUsername(), roleKeys);
        String newRefresh = jwtTokenUtil.generateRefreshToken(userId);

        List<SysMenu> menus = userService.getMenusByUserId(userId);
        List<RouterVO> routerTree = buildRouterTree(menus);

        return Result.success(LoginResponse.builder()
                .accessToken(newToken)
                .refreshToken(newRefresh)
                .user(LoginResponse.UserInfo.builder()
                        .userId(user.getUserId())
                        .username(user.getUsername())
                        .nickname(user.getNickname())
                        .avatar(user.getAvatar())
                        .roles(roleKeys)
                        .menus(routerTree)
                        .build())
                .build());
    }

    /** 登出 */
    @PostMapping("/logout")
    public Result<Void> logout() {
        SecurityContextHolder.clearContext();
        return Result.success();
    }


    /** 邮箱注册 */
    @PostMapping("/register")
    @Log(title = "用户注册", operType = 5)
    public Result<LoginResponse> register(@RequestBody @Valid RegisterRequest request, HttpServletRequest httpReq) {
        if (!request.getPassword().equals(request.getConfirmPassword())) {
            return Result.error(400, "两次输入的密码不一致");
        }
        SysUser existingUser = userService.getUserByUsername(request.getUsername());
        if (existingUser != null) {
            return Result.error(400, "用户名已存在");
        }
        SysUser existingEmail = userService.getUserByEmail(request.getEmail());
        if (existingEmail != null) {
            return Result.error(400, "邮箱已被注册");
        }
        SysUser newUser = new SysUser();
        newUser.setUsername(request.getUsername());
        newUser.setPassword(passwordEncoder.encode(request.getPassword()));
        newUser.setEmail(request.getEmail());
        newUser.setNickname(request.getUsername());
        newUser.setStatus(0);
        newUser.setDeptId(1L);
        newUser.setCreatedAt(LocalDateTime.now());
        newUser.setUpdatedAt(LocalDateTime.now());
        userService.saveUser(newUser);
        userService.assignDefaultRole(newUser.getUserId());

        List<String> roles = List.of("user");
        String accessToken = jwtTokenUtil.generateToken(newUser.getUserId(), newUser.getUsername(), roles);
        String refreshToken = jwtTokenUtil.generateRefreshToken(newUser.getUserId());

        LoginResponse.UserInfo userInfo = LoginResponse.UserInfo.builder()
                .userId(newUser.getUserId())
                .username(newUser.getUsername())
                .nickname(newUser.getNickname())
                .email(newUser.getEmail())
                .roles(roles)
                .menus(List.of())
                .build();

        SysLoginLog loginLog = new SysLoginLog();
        loginLog.setUsername(request.getUsername());
        loginLog.setIp(httpReq.getRemoteAddr());
        loginLog.setLoginTime(LocalDateTime.now());
        loginLog.setStatus(0);
        loginLog.setMsg("注册成功");
        userService.saveLoginLog(loginLog);

        return Result.success(LoginResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .user(userInfo)
                .build());
    }

    /** Google OAuth 登录/注册 */
    @PostMapping("/oauth/google")
    public Result<LoginResponse> googleOauth(@RequestBody Map<String, String> body, HttpServletRequest httpReq) {
        String idToken = body.get("idToken");
        if (idToken == null || idToken.isBlank()) {
            return Result.error(400, "缺少 Google ID Token");
        }
        String email = body.getOrDefault("email", "");
        String name = body.getOrDefault("name", "GoogleUser");

        SysUser user = userService.getUserByEmail(email);
        if (user == null) {
            user = new SysUser();
            user.setUsername("google_" + email.replaceAll("@.*", ""));
            user.setPassword(passwordEncoder.encode(UUID.randomUUID().toString()));
            user.setEmail(email);
            user.setNickname(name);
            user.setStatus(0);
            user.setDeptId(1L);
            user.setCreatedAt(LocalDateTime.now());
            user.setUpdatedAt(LocalDateTime.now());
            userService.saveUser(user);
            userService.assignDefaultRole(user.getUserId());
        }

        List<String> roles = List.of("user");
        if (user.getUserId() == 1L) roles = List.of("admin");
        String accessToken = jwtTokenUtil.generateToken(user.getUserId(), user.getUsername(), roles);
        String refreshToken = jwtTokenUtil.generateRefreshToken(user.getUserId());

        LoginResponse.UserInfo userInfo = LoginResponse.UserInfo.builder()
                .userId(user.getUserId())
                .username(user.getUsername())
                .nickname(user.getNickname())
                .email(user.getEmail())
                .roles(roles)
                .menus(List.of())
                .build();

        return Result.success(LoginResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .user(userInfo)
                .build());
    }

    /** 生成验证码 */
    @GetMapping("/captcha")
    public Result<Object> captcha() {
        // Simple captcha mock — 生产环境应集成 EasyCaptcha / Google Kaptcha
        String uuid = UUID.randomUUID().toString().replace("-", "");
        return Result.success(java.util.Map.of("uuid", uuid, "img", "data:image/png;base64,iVBORw0KGgoAAAANSUhEUg..."));
    }

    // ============ 内部方法 ============

    /** 将 SysMenu 列表转为前端 RouterVO 树 */
    private List<RouterVO> buildRouterTree(List<SysMenu> menus) {
        List<RouterVO> roots = new ArrayList<>();
        for (SysMenu m : menus) {
            if (m.getParentId() == 0) {
                roots.add(convertToRouter(m, menus));
            }
        }
        return roots;
    }

    private RouterVO convertToRouter(SysMenu menu, List<SysMenu> allMenus) {
        String component = menu.getComponent();
        if (component == null || component.isEmpty()) {
            component = menu.getMenuType() == 0 ? "Layout" : null;
        }
        RouterVO router = RouterVO.builder()
                .id(menu.getMenuId())
                .parentId(menu.getParentId())
                .name(menu.getMenuName())
                .path(menu.getPath())
                .component(component)
                .meta(RouterVO.Meta.builder()
                        .title(menu.getMenuName())
                        .icon(menu.getIcon())
                        .hideMenu(menu.getVisible() != 0)
                        .build())
                .children(new ArrayList<>())
                .build();
        for (SysMenu child : allMenus) {
            if (child.getParentId().equals(menu.getMenuId())) {
                router.getChildren().add(convertToRouter(child, allMenus));
            }
        }
        return router;
    }

    @GetMapping("/info")
    public Result<String> info() {
        return Result.success("admin auth service is running");
    }
}
