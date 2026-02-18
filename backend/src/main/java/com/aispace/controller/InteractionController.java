package com.aispace.controller;

import com.aispace.dto.response.ApiResponse;
import com.aispace.dto.response.PageResponse;
import com.aispace.entity.Interaction;
import com.aispace.service.InteractionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 交互记录控制器
 */
@RestController
@RequestMapping("/api/v1/interactions")
@RequiredArgsConstructor
@Tag(name = "交互记录", description = "客户交互记录管理接口")
public class InteractionController {
    
    private final InteractionService interactionService;
    
    /**
     * 获取客户的交互记录
     */
    @GetMapping("/customer/{customerId}")
    @Operation(summary = "获取客户交互记录")
    public ApiResponse<PageResponse<Interaction>> listByCustomer(
            @PathVariable UUID customerId,
            @RequestParam(required = false) String type,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize
    ) {
        PageResponse<Interaction> result;
        
        if (type != null && !type.isBlank()) {
            Interaction.InteractionType interactionType = Interaction.InteractionType.valueOf(
                type.toUpperCase()
            );
            result = interactionService.listByType(customerId, interactionType, page, pageSize);
        } else {
            result = interactionService.listByCustomer(customerId, page, pageSize);
        }
        
        return ApiResponse.success(result);
    }
    
    /**
     * 获取项目的交互记录
     */
    @GetMapping("/project/{projectId}")
    @Operation(summary = "获取项目交互记录")
    public ApiResponse<PageResponse<Interaction>> listByProject(
            @PathVariable UUID projectId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize
    ) {
        PageResponse<Interaction> result = interactionService.listByProject(projectId, page, pageSize);
        return ApiResponse.success(result);
    }
    
    /**
     * 获取交互详情
     */
    @GetMapping("/{id}")
    @Operation(summary = "获取交互详情")
    public ApiResponse<Interaction> getInteraction(@PathVariable UUID id) {
        return interactionService.getInteraction(id)
            .map(ApiResponse::success)
            .orElse(ApiResponse.notFound("交互记录不存在"));
    }
    
    /**
     * 创建交互记录
     */
    @PostMapping
    @Operation(summary = "创建交互记录")
    public ApiResponse<Interaction> createInteraction(@RequestBody Interaction interaction) {
        Interaction created = interactionService.createInteraction(interaction);
        return ApiResponse.success("交互记录创建成功", created);
    }
    
    /**
     * 更新交互记录
     */
    @PutMapping("/{id}")
    @Operation(summary = "更新交互记录")
    public ApiResponse<Interaction> updateInteraction(
            @PathVariable UUID id,
            @RequestBody Interaction interaction
    ) {
        Interaction updated = interactionService.updateInteraction(id, interaction);
        return ApiResponse.success("交互记录更新成功", updated);
    }
    
    /**
     * 删除交互记录
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "删除交互记录")
    public ApiResponse<Void> deleteInteraction(@PathVariable UUID id) {
        interactionService.deleteInteraction(id);
        return ApiResponse.success("交互记录删除成功", null);
    }
    
    /**
     * 获取最近交互
     */
    @GetMapping("/customer/{customerId}/recent")
    @Operation(summary = "获取最近交互")
    public ApiResponse<List<Interaction>> getRecentInteractions(@PathVariable UUID customerId) {
        List<Interaction> interactions = interactionService.getRecentInteractions(customerId);
        return ApiResponse.success(interactions);
    }
    
    /**
     * 获取待跟进事项
     */
    @GetMapping("/pending-followups")
    @Operation(summary = "获取待跟进事项")
    public ApiResponse<List<Interaction>> getPendingFollowUps(
            @RequestParam(required = false) 
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime deadline
    ) {
        LocalDateTime effectiveDeadline = deadline != null ? deadline : LocalDateTime.now().plusDays(7);
        List<Interaction> interactions = interactionService.getPendingFollowUps(effectiveDeadline);
        return ApiResponse.success(interactions);
    }
    
    /**
     * 获取交互类型统计
     */
    @GetMapping("/customer/{customerId}/stats/types")
    @Operation(summary = "获取交互类型统计")
    public ApiResponse<Map<String, Long>> getTypeStats(@PathVariable UUID customerId) {
        Map<String, Long> stats = interactionService.getTypeStats(customerId);
        return ApiResponse.success(stats);
    }
    
    /**
     * 获取情感统计
     */
    @GetMapping("/customer/{customerId}/stats/sentiment")
    @Operation(summary = "获取情感统计")
    public ApiResponse<Map<String, Long>> getSentimentStats(@PathVariable UUID customerId) {
        Map<String, Long> stats = interactionService.getSentimentStats(customerId);
        return ApiResponse.success(stats);
    }
}
