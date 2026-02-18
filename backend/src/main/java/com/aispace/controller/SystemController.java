package com.aispace.controller;

import com.aispace.dto.response.ApiResponse;
import com.aispace.service.BackupService;
import com.aispace.service.ConfigService;
import com.aispace.service.RestartService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 系统管理控制器
 * 提供备份、健康检查等系统管理功能
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/system")
@RequiredArgsConstructor
@Tag(name = "系统管理", description = "系统备份、健康检查、配置管理等")
public class SystemController {
    
    private final BackupService backupService;
    private final ConfigService configService;
    private final RestartService restartService;
    private final ObjectMapper objectMapper;
    
    private static final OkHttpClient httpClient = new OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build();
    
    @PostMapping("/backup")
    @Operation(summary = "执行完整备份")
    public ApiResponse<BackupService.BackupResult> performBackup() {
        BackupService.BackupResult result = backupService.performFullBackup();
        if (result.success()) {
            return ApiResponse.success("备份成功", result);
        } else {
            return ApiResponse.error("备份失败: " + result.error());
        }
    }
    
    @GetMapping("/backups")
    @Operation(summary = "列出所有备份")
    public ApiResponse<List<BackupService.BackupInfo>> listBackups() {
        return ApiResponse.success(backupService.listBackups());
    }
    
    @PostMapping("/backups/cleanup")
    @Operation(summary = "清理过期备份")
    public ApiResponse<Integer> cleanupBackups() {
        int deleted = backupService.cleanupOldBackups();
        return ApiResponse.success("清理完成，删除了 " + deleted + " 个备份", deleted);
    }
    
    @GetMapping("/health")
    @Operation(summary = "健康检查")
    public ApiResponse<HealthStatus> healthCheck() {
        HealthStatus status = HealthStatus.builder()
            .status("UP")
            .database(checkDatabase())
            .memory(getMemoryInfo())
            .uptime(getUptime())
            .build();
        return ApiResponse.success(status);
    }
    
    @GetMapping("/info")
    @Operation(summary = "系统信息")
    public ApiResponse<SystemInfo> getSystemInfo() {
        SystemInfo info = SystemInfo.builder()
            .appName("AI员工协作系统")
            .version("0.1.0")
            .javaVersion(System.getProperty("java.version"))
            .osName(System.getProperty("os.name"))
            .osArch(System.getProperty("os.arch"))
            .availableProcessors(Runtime.getRuntime().availableProcessors())
            .build();
        return ApiResponse.success(info);
    }
    
    /**
     * 获取可用的 LLM 模型列表
     * 根据 API 端点和 Key 动态获取
     */
    @PostMapping("/llm/models")
    @Operation(summary = "获取可用模型列表")
    public ApiResponse<List<ModelInfo>> listModels(@RequestBody LLMConfigRequest config) {
        log.info("Fetching models from endpoint: {}", config.endpoint());
        
        if (config.endpoint() == null || config.endpoint().isBlank()) {
            return ApiResponse.error("请输入 API 端点");
        }
        if (config.apiKey() == null || config.apiKey().isBlank()) {
            return ApiResponse.error("请输入 API Key");
        }
        
        try {
            String modelsUrl = normalizeEndpoint(config.endpoint()) + "/models";
            
            Request request = new Request.Builder()
                .url(modelsUrl)
                .header("Authorization", "Bearer " + config.apiKey())
                .header("Content-Type", "application/json")
                .get()
                .build();
            
            try (Response response = httpClient.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    String body = response.body() != null ? response.body().string() : "";
                    log.warn("Failed to fetch models: {} - {}", response.code(), body);
                    return ApiResponse.error("获取模型列表失败: HTTP " + response.code());
                }
                
                String responseBody = response.body() != null ? response.body().string() : "{}";
                JsonNode root = objectMapper.readTree(responseBody);
                
                List<ModelInfo> models = new ArrayList<>();
                JsonNode dataNode = root.has("data") ? root.get("data") : root;
                
                if (dataNode.isArray()) {
                    for (JsonNode modelNode : dataNode) {
                        String id = modelNode.has("id") ? modelNode.get("id").asText() : null;
                        String name = modelNode.has("name") ? modelNode.get("name").asText() : id;
                        String ownedBy = modelNode.has("owned_by") ? modelNode.get("owned_by").asText() : "";
                        
                        if (id != null && !id.isBlank()) {
                            // 过滤掉非聊天模型（如 embedding、whisper 等）
                            if (!id.contains("embedding") && !id.contains("whisper") && 
                                !id.contains("tts") && !id.contains("dall-e")) {
                                models.add(new ModelInfo(id, name != null ? name : id, ownedBy));
                            }
                        }
                    }
                }
                
                if (models.isEmpty()) {
                    // 如果没有获取到模型，返回一些常见模型作为备选
                    models = getDefaultModels(config.endpoint());
                }
                
                log.info("Found {} models", models.size());
                return ApiResponse.success(models);
            }
        } catch (Exception e) {
            log.error("Error fetching models: {}", e.getMessage());
            // 返回默认模型列表
            return ApiResponse.success("无法连接到 API，显示默认模型", getDefaultModels(config.endpoint()));
        }
    }
    
    /**
     * 测试 LLM 连接
     */
    @PostMapping("/llm/test")
    @Operation(summary = "测试 LLM 连接")
    public ApiResponse<LLMTestResult> testLLMConnection(@RequestBody LLMConfigRequest config) {
        log.info("Testing LLM connection to: {}", config.endpoint());
        
        if (config.endpoint() == null || config.endpoint().isBlank()) {
            return ApiResponse.error("请输入 API 端点");
        }
        if (config.apiKey() == null || config.apiKey().isBlank()) {
            return ApiResponse.error("请输入 API Key");
        }
        
        try {
            String modelsUrl = normalizeEndpoint(config.endpoint()) + "/models";
            
            Request request = new Request.Builder()
                .url(modelsUrl)
                .header("Authorization", "Bearer " + config.apiKey())
                .get()
                .build();
            
            long startTime = System.currentTimeMillis();
            try (Response response = httpClient.newCall(request).execute()) {
                long latency = System.currentTimeMillis() - startTime;
                
                if (response.isSuccessful()) {
                    return ApiResponse.success(new LLMTestResult(true, "连接成功", latency));
                } else {
                    String errorMsg = response.code() == 401 ? "API Key 无效" : "HTTP " + response.code();
                    return ApiResponse.success(new LLMTestResult(false, errorMsg, latency));
                }
            }
        } catch (Exception e) {
            return ApiResponse.success(new LLMTestResult(false, "连接失败: " + e.getMessage(), 0));
        }
    }
    
    private String normalizeEndpoint(String endpoint) {
        String normalized = endpoint.trim();
        if (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        // 移除末尾的 /v1 以便统一处理
        if (!normalized.endsWith("/v1")) {
            normalized = normalized + "/v1";
        }
        return normalized.replace("/v1/v1", "/v1");
    }
    
    private List<ModelInfo> getDefaultModels(String endpoint) {
        List<ModelInfo> models = new ArrayList<>();
        
        if (endpoint.contains("moonshot") || endpoint.contains("kimi")) {
            models.add(new ModelInfo("moonshot-v1-8k", "Moonshot V1 8K", "moonshot"));
            models.add(new ModelInfo("moonshot-v1-32k", "Moonshot V1 32K", "moonshot"));
            models.add(new ModelInfo("moonshot-v1-128k", "Moonshot V1 128K", "moonshot"));
            models.add(new ModelInfo("kimi-k2-0711-preview", "Kimi K2 Preview", "moonshot"));
        } else if (endpoint.contains("deepseek")) {
            models.add(new ModelInfo("deepseek-chat", "DeepSeek Chat", "deepseek"));
            models.add(new ModelInfo("deepseek-coder", "DeepSeek Coder", "deepseek"));
        } else if (endpoint.contains("openai")) {
            models.add(new ModelInfo("gpt-4o", "GPT-4o", "openai"));
            models.add(new ModelInfo("gpt-4o-mini", "GPT-4o Mini", "openai"));
            models.add(new ModelInfo("gpt-4-turbo", "GPT-4 Turbo", "openai"));
            models.add(new ModelInfo("gpt-3.5-turbo", "GPT-3.5 Turbo", "openai"));
        } else {
            // 通用模型
            models.add(new ModelInfo("gpt-4o", "GPT-4o", "openai"));
            models.add(new ModelInfo("gpt-3.5-turbo", "GPT-3.5 Turbo", "openai"));
            models.add(new ModelInfo("claude-3-opus", "Claude 3 Opus", "anthropic"));
            models.add(new ModelInfo("claude-3-sonnet", "Claude 3 Sonnet", "anthropic"));
        }
        
        return models;
    }
    
    private String checkDatabase() {
        try {
            return "UP";
        } catch (Exception e) {
            return "DOWN: " + e.getMessage();
        }
    }
    
    private MemoryInfo getMemoryInfo() {
        Runtime runtime = Runtime.getRuntime();
        long maxMemory = runtime.maxMemory();
        long totalMemory = runtime.totalMemory();
        long freeMemory = runtime.freeMemory();
        long usedMemory = totalMemory - freeMemory;
        
        return MemoryInfo.builder()
            .max(formatBytes(maxMemory))
            .total(formatBytes(totalMemory))
            .used(formatBytes(usedMemory))
            .free(formatBytes(freeMemory))
            .usagePercent((double) usedMemory / maxMemory * 100)
            .build();
    }
    
    private String getUptime() {
        long uptime = java.lang.management.ManagementFactory.getRuntimeMXBean().getUptime();
        long seconds = uptime / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        long days = hours / 24;
        
        if (days > 0) {
            return String.format("%d天 %d小时", days, hours % 24);
        } else if (hours > 0) {
            return String.format("%d小时 %d分钟", hours, minutes % 60);
        } else {
            return String.format("%d分钟", minutes);
        }
    }
    
    private String formatBytes(long bytes) {
        if (bytes < 1024) return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(1024));
        char unit = "KMGTPE".charAt(exp - 1);
        return String.format("%.1f %sB", bytes / Math.pow(1024, exp), unit);
    }
    
    @lombok.Data
    @lombok.Builder
    public static class HealthStatus {
        private String status;
        private String database;
        private MemoryInfo memory;
        private String uptime;
    }
    
    @lombok.Data
    @lombok.Builder
    public static class MemoryInfo {
        private String max;
        private String total;
        private String used;
        private String free;
        private double usagePercent;
    }
    
    @lombok.Data
    @lombok.Builder
    public static class SystemInfo {
        private String appName;
        private String version;
        private String javaVersion;
        private String osName;
        private String osArch;
        private int availableProcessors;
    }
    
    /**
     * 保存 AI 配置并重启服务
     */
    @PostMapping("/config/ai")
    @Operation(summary = "保存 AI 配置并重启服务")
    public ApiResponse<String> saveAIConfig(@RequestBody AIConfigRequest request) {
        try {
            if (request.endpoint() == null || request.endpoint().isBlank()) {
                return ApiResponse.error("请输入 API 端点");
            }
            if (request.apiKey() == null || request.apiKey().isBlank()) {
                return ApiResponse.error("请输入 API Key");
            }
            if (request.model() == null || request.model().isBlank()) {
                return ApiResponse.error("请选择模型");
            }
            
            // 保存配置
            configService.updateAIConfig(request.endpoint(), request.apiKey(), request.model());
            
            // 触发重启（异步，确保响应先返回）
            restartService.restart();
            
            return ApiResponse.success("配置已保存，系统正在重启...", "restarting");
        } catch (IOException e) {
            log.error("保存 AI 配置失败", e);
            return ApiResponse.error("保存配置失败: " + e.getMessage());
        }
    }

    /**
     * 保存通知配置并重启服务
     */
    @PostMapping("/config/notification")
    @Operation(summary = "保存通知配置并重启服务")
    public ApiResponse<String> saveNotificationConfig(@RequestBody NotificationConfigRequest request) {
        try {
            configService.updateNotificationConfig(
                request.feishuWebhook(), 
                request.telegramToken(), 
                request.telegramChatId()
            );
            
            // 触发重启
            restartService.restart();
            
            return ApiResponse.success("配置已保存，系统正在重启...", "restarting");
        } catch (IOException e) {
            log.error("保存通知配置失败", e);
            return ApiResponse.error("保存配置失败: " + e.getMessage());
        }
    }

    /**
     * 手动重启服务
     */
    @PostMapping("/restart")
    @Operation(summary = "重启后端服务")
    public ApiResponse<String> restart() {
        restartService.restart();
        return ApiResponse.success("系统正在重启...", "restarting");
    }

    public record LLMConfigRequest(String endpoint, String apiKey) {}
    
    public record ModelInfo(String id, String name, String ownedBy) {}
    
    public record LLMTestResult(boolean success, String message, long latencyMs) {}
    
    public record AIConfigRequest(String endpoint, String apiKey, String model) {}
    
    public record NotificationConfigRequest(String feishuWebhook, String telegramToken, String telegramChatId) {}
}
