package com.aispace.controller;

import com.aispace.dto.response.ApiResponse;
import com.aispace.entity.Reminder;
import com.aispace.service.ReminderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * 提醒管理控制器
 */
@RestController
@RequestMapping("/api/v1/reminders")
@RequiredArgsConstructor
@Tag(name = "提醒管理", description = "提醒创建和状态管理接口")
public class ReminderController {
    
    private final ReminderService reminderService;
    
    /**
     * 创建提醒
     */
    @PostMapping
    @Operation(summary = "创建提醒")
    public ApiResponse<Reminder> createReminder(@RequestBody Reminder reminder) {
        Reminder created = reminderService.createReminder(reminder);
        return ApiResponse.success("提醒创建成功", created);
    }
    
    /**
     * 获取提醒详情
     */
    @GetMapping("/{id}")
    @Operation(summary = "获取提醒详情")
    public ApiResponse<Reminder> getReminder(@PathVariable UUID id) {
        return reminderService.getReminder(id)
            .map(ApiResponse::success)
            .orElse(ApiResponse.notFound("提醒不存在"));
    }
    
    /**
     * 获取今日提醒
     */
    @GetMapping("/today")
    @Operation(summary = "获取今日提醒")
    public ApiResponse<List<Reminder>> getTodayReminders() {
        List<Reminder> reminders = reminderService.getTodayReminders();
        return ApiResponse.success(reminders);
    }
    
    /**
     * 获取即将到期的提醒
     */
    @GetMapping("/upcoming")
    @Operation(summary = "获取即将到期的提醒")
    public ApiResponse<List<Reminder>> getUpcomingReminders(
            @RequestParam(defaultValue = "24") int hours,
            @RequestParam(defaultValue = "10") int limit
    ) {
        List<Reminder> reminders = reminderService.getUpcomingReminders(hours, limit);
        return ApiResponse.success(reminders);
    }
    
    /**
     * 获取项目提醒
     */
    @GetMapping("/project/{projectId}")
    @Operation(summary = "获取项目提醒")
    public ApiResponse<List<Reminder>> getProjectReminders(@PathVariable UUID projectId) {
        List<Reminder> reminders = reminderService.getProjectReminders(projectId);
        return ApiResponse.success(reminders);
    }
    
    /**
     * 获取客户提醒
     */
    @GetMapping("/customer/{customerId}")
    @Operation(summary = "获取客户提醒")
    public ApiResponse<List<Reminder>> getCustomerReminders(@PathVariable UUID customerId) {
        List<Reminder> reminders = reminderService.getCustomerReminders(customerId);
        return ApiResponse.success(reminders);
    }
    
    /**
     * 完成提醒
     */
    @PatchMapping("/{id}/complete")
    @Operation(summary = "完成提醒")
    public ApiResponse<Void> completeReminder(@PathVariable UUID id) {
        reminderService.completeReminder(id);
        return ApiResponse.success("提醒已完成", null);
    }
    
    /**
     * 忽略提醒
     */
    @PatchMapping("/{id}/dismiss")
    @Operation(summary = "忽略提醒")
    public ApiResponse<Void> dismissReminder(@PathVariable UUID id) {
        reminderService.dismissReminder(id);
        return ApiResponse.success("提醒已忽略", null);
    }
    
    /**
     * 推迟提醒
     */
    @PatchMapping("/{id}/snooze")
    @Operation(summary = "推迟提醒")
    public ApiResponse<Void> snoozeReminder(
            @PathVariable UUID id,
            @RequestBody SnoozeRequest request
    ) {
        reminderService.snoozeReminder(id, request.minutes());
        return ApiResponse.success("提醒已推迟", null);
    }
    
    /**
     * 删除提醒
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "删除提醒")
    public ApiResponse<Void> deleteReminder(@PathVariable UUID id) {
        reminderService.deleteReminder(id);
        return ApiResponse.success("提醒已删除", null);
    }
    
    /**
     * 推迟请求
     */
    public record SnoozeRequest(int minutes) {}
}
