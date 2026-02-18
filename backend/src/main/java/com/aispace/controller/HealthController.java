package com.aispace.controller;

import com.aispace.dto.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 健康检查控制器
 */
@RestController
@RequestMapping("/api/v1")
@Tag(name = "系统", description = "系统状态和健康检查")
public class HealthController {
    
    /**
     * 健康检查
     */
    @GetMapping("/health")
    @Operation(summary = "健康检查")
    public ApiResponse<Map<String, Object>> health() {
        Map<String, Object> healthInfo = Map.of(
            "status", "UP",
            "service", "aispace-backend",
            "version", "0.1.0",
            "timestamp", LocalDateTime.now().toString()
        );
        return ApiResponse.success(healthInfo);
    }
    
    /**
     * 系统信息
     */
    @GetMapping("/info")
    @Operation(summary = "系统信息")
    public ApiResponse<Map<String, Object>> info() {
        Map<String, Object> systemInfo = Map.of(
            "name", "AI员工协作系统",
            "version", "0.1.0",
            "description", "面向政企智慧城市行业的AI协作平台",
            "java", System.getProperty("java.version"),
            "os", System.getProperty("os.name")
        );
        return ApiResponse.success(systemInfo);
    }
}
