package com.futures.account.controller;

import com.futures.account.service.AccountService;
import com.futures.common.result.Result;
import com.futures.common.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/v1/account")
@RequiredArgsConstructor
public class AccountController {

    private final AccountService accountService;

    @GetMapping("/overview")
    public Result<Map<String, Object>> getOverview(
            @RequestHeader(value = "X-User-Id", required = false) Long headerUserId,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        
        Long userId = headerUserId;
        
        // 如果 X-User-Id 未提供，从 JWT 中提取
        if (userId == null && authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            userId = JwtUtil.getUserId(token);
            log.info("从 JWT 提取 userId: {}", userId);
        }
        
        if (userId == null) {
            return Result.error(400, "无法识别用户身份");
        }
        
        return Result.success(accountService.getAccountOverview(userId));
    }
}
