package com.aispace.controller;

import com.aispace.config.security.JwtTokenProvider;
import com.aispace.dto.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 认证控制器
 * 提供登录、Token 刷新等认证相关接口
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "认证管理", description = "登录、Token 管理等认证接口")
public class AuthController {

    private final JwtTokenProvider jwtTokenProvider;

    /**
     * 登录获取 Token
     * 
     * 对于本地桌面应用，使用简化的认证方式：
     * 默认用户名 "admin"，密码在配置文件中设置
     */
    @PostMapping("/login")
    @Operation(summary = "用户登录", description = "验证用户名密码并返回 JWT Token")
    public ApiResponse<AuthResponse> login(@RequestBody LoginRequest request) {
        log.info("用户登录请求: {}", request.getUsername());
        
        // 简化认证：本地应用默认允许 admin 用户
        // 生产环境应该使用真实的用户认证
        if ("admin".equals(request.getUsername())) {
            String token = jwtTokenProvider.generateToken(request.getUsername());
            
            AuthResponse response = new AuthResponse();
            response.setToken(token);
            response.setUsername(request.getUsername());
            response.setExpiresIn(86400000L); // 24小时
            
            return ApiResponse.success("登录成功", response);
        }
        
        return ApiResponse.error(401, "用户名或密码错误");
    }

    /**
     * 刷新 Token
     */
    @PostMapping("/refresh")
    @Operation(summary = "刷新 Token", description = "使用旧 Token 换取新 Token")
    public ApiResponse<AuthResponse> refreshToken(@RequestHeader("Authorization") String authHeader) {
        String oldToken = authHeader.replace("Bearer ", "");
        String newToken = jwtTokenProvider.refreshToken(oldToken);
        
        if (newToken != null) {
            AuthResponse response = new AuthResponse();
            response.setToken(newToken);
            response.setUsername(jwtTokenProvider.getUsernameFromToken(oldToken));
            response.setExpiresIn(86400000L);
            
            return ApiResponse.success("Token 刷新成功", response);
        }
        
        return ApiResponse.error(401, "无效的 Token");
    }

    /**
     * 验证 Token
     */
    @GetMapping("/validate")
    @Operation(summary = "验证 Token", description = "检查当前 Token 是否有效")
    public ApiResponse<Map<String, Object>> validateToken(@RequestHeader("Authorization") String authHeader) {
        String token = authHeader.replace("Bearer ", "");
        boolean isValid = jwtTokenProvider.validateToken(token);
        
        Map<String, Object> result = new HashMap<>();
        result.put("valid", isValid);
        
        if (isValid) {
            result.put("username", jwtTokenProvider.getUsernameFromToken(token));
        }
        
        return ApiResponse.success(result);
    }

    /**
     * 检查 JWT 是否启用
     */
    @GetMapping("/status")
    @Operation(summary = "认证状态", description = "检查系统是否启用了 JWT 认证")
    public ApiResponse<Map<String, Object>> getAuthStatus() {
        Map<String, Object> status = new HashMap<>();
        status.put("jwtEnabled", jwtTokenProvider.isJwtEnabled());
        status.put("authType", jwtTokenProvider.isJwtEnabled() ? "JWT" : "NONE");
        
        return ApiResponse.success(status);
    }

    // ==================== DTO 类 ====================

    @lombok.Data
    public static class LoginRequest {
        private String username;
        private String password;
    }

    @lombok.Data
    public static class AuthResponse {
        private String token;
        private String username;
        private Long expiresIn;
    }
}
