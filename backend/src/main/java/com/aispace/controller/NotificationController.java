package com.aispace.controller;

import com.aispace.dto.response.ApiResponse;
import com.aispace.service.notification.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * 通知管理控制器
 * 管理消息推送渠道和配置
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
@Tag(name = "通知管理", description = "消息推送渠道配置和管理")
public class NotificationController {
    
    private final NotificationService notificationService;
    
    @GetMapping("/channels/status")
    @Operation(summary = "获取通知渠道状态")
    public ApiResponse<Map<String, Boolean>> getChannelStatus() {
        return ApiResponse.success(notificationService.getChannelStatus());
    }
    
    @GetMapping("/channels/enabled")
    @Operation(summary = "获取已启用的通知渠道")
    public ApiResponse<List<String>> getEnabledChannels() {
        return ApiResponse.success(notificationService.getEnabledChannels());
    }
    
    @PutMapping("/channels/enabled")
    @Operation(summary = "设置启用的通知渠道")
    public ApiResponse<Void> setEnabledChannels(@RequestBody EnableChannelsRequest request) {
        notificationService.setEnabledChannels(request.channels());
        return ApiResponse.success("通知渠道已更新", null);
    }
    
    @PostMapping("/test")
    @Operation(summary = "测试所有通知渠道")
    public ApiResponse<Map<String, Boolean>> testChannels() {
        try {
            Map<String, Boolean> results = notificationService.testAllChannels().get();
            return ApiResponse.success("测试完成", results);
        } catch (Exception e) {
            log.error("Test failed", e);
            return ApiResponse.error("测试失败: " + e.getMessage());
        }
    }
    
    @PostMapping("/test/{channel}")
    @Operation(summary = "测试指定通知渠道")
    public ApiResponse<Boolean> testChannel(@PathVariable String channel) {
        try {
            Boolean result = notificationService.sendToChannel(channel, "测试消息 - AI员工协作系统").get();
            return ApiResponse.success(result ? "发送成功" : "发送失败", result);
        } catch (Exception e) {
            log.error("Test channel failed: {}", channel, e);
            return ApiResponse.error("测试失败: " + e.getMessage());
        }
    }
    
    @PostMapping("/send")
    @Operation(summary = "发送自定义通知")
    public ApiResponse<Map<String, Boolean>> sendNotification(@RequestBody SendNotificationRequest request) {
        try {
            Map<String, Boolean> results;
            if (request.actions() != null && !request.actions().isEmpty()) {
                results = notificationService.sendCard(request.title(), request.content(), request.actions()).get();
            } else {
                String message = request.title() != null 
                    ? request.title() + "\n\n" + request.content() 
                    : request.content();
                results = notificationService.sendText(message).get();
            }
            return ApiResponse.success("发送完成", results);
        } catch (Exception e) {
            log.error("Send notification failed", e);
            return ApiResponse.error("发送失败: " + e.getMessage());
        }
    }
    
    // ========== DTO ==========
    
    public record EnableChannelsRequest(List<String> channels) {}
    
    public record SendNotificationRequest(
        String title,
        String content,
        Map<String, String> actions
    ) {}
}
