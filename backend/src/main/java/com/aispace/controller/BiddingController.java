package com.aispace.controller;

import com.aispace.dto.response.ApiResponse;
import com.aispace.dto.response.PageResponse;
import com.aispace.entity.BiddingItem;
import com.aispace.entity.BiddingSource;
import com.aispace.service.BiddingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 招标监控控制器
 * 提供招标信息采集、查询和管理接口
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/bidding")
@RequiredArgsConstructor
@Tag(name = "招标监控", description = "招标信息采集和监控管理")
public class BiddingController {
    
    private final BiddingService biddingService;
    
    // ========== 数据源管理 ==========
    
    @GetMapping("/sources")
    @Operation(summary = "获取所有数据源")
    public ApiResponse<List<BiddingSource>> listSources() {
        return ApiResponse.success(biddingService.getAllSources());
    }
    
    @GetMapping("/sources/active")
    @Operation(summary = "获取活跃数据源")
    public ApiResponse<List<BiddingSource>> listActiveSources() {
        return ApiResponse.success(biddingService.getActiveSources());
    }
    
    @GetMapping("/sources/{id}")
    @Operation(summary = "获取数据源详情")
    public ApiResponse<BiddingSource> getSource(@PathVariable UUID id) {
        return biddingService.getSource(id)
            .map(ApiResponse::success)
            .orElse(ApiResponse.notFound("数据源不存在"));
    }
    
    @PostMapping("/sources")
    @Operation(summary = "创建数据源")
    public ApiResponse<BiddingSource> createSource(@RequestBody BiddingSource source) {
        return ApiResponse.success("数据源创建成功", biddingService.createSource(source));
    }
    
    @PutMapping("/sources/{id}")
    @Operation(summary = "更新数据源")
    public ApiResponse<BiddingSource> updateSource(@PathVariable UUID id, @RequestBody BiddingSource source) {
        return ApiResponse.success("数据源更新成功", biddingService.updateSource(id, source));
    }
    
    @DeleteMapping("/sources/{id}")
    @Operation(summary = "删除数据源")
    public ApiResponse<Void> deleteSource(@PathVariable UUID id) {
        biddingService.deleteSource(id);
        return ApiResponse.success("数据源已删除", null);
    }
    
    @PostMapping("/sources/{id}/crawl")
    @Operation(summary = "手动触发数据采集")
    public ApiResponse<Map<String, Object>> crawlSource(@PathVariable UUID id) {
        int count = biddingService.crawlSource(id);
        return ApiResponse.success("采集完成", Map.of("newItems", count));
    }
    
    // ========== 招标信息管理 ==========
    
    @GetMapping("/items")
    @Operation(summary = "获取招标信息列表")
    public ApiResponse<PageResponse<BiddingItem>> listItems(
            @RequestParam(required = false) String region,
            @RequestParam(required = false) String industry,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Boolean matched,
            @RequestParam(required = false) Boolean unread,
            @RequestParam(required = false) Boolean starred,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        
        PageResponse<BiddingItem> result = biddingService.listItems(
            region, industry, keyword, matched, unread, starred, page, pageSize);
        return ApiResponse.success(result);
    }
    
    @GetMapping("/items/{id}")
    @Operation(summary = "获取招标信息详情")
    public ApiResponse<BiddingItem> getItem(@PathVariable UUID id) {
        return biddingService.getItem(id)
            .map(item -> {
                // 自动标记为已读
                biddingService.markAsRead(id);
                return ApiResponse.success(item);
            })
            .orElse(ApiResponse.notFound("招标信息不存在"));
    }
    
    @PostMapping("/items")
    @Operation(summary = "手动添加招标信息")
    public ApiResponse<BiddingItem> createItem(@RequestBody BiddingItem item) {
        return ApiResponse.success("招标信息添加成功", biddingService.createItem(item));
    }
    
    @PostMapping("/items/{id}/read")
    @Operation(summary = "标记为已读")
    public ApiResponse<Void> markAsRead(@PathVariable UUID id) {
        biddingService.markAsRead(id);
        return ApiResponse.success("已标记为已读", null);
    }
    
    @PostMapping("/items/{id}/star")
    @Operation(summary = "切换收藏状态")
    public ApiResponse<Void> toggleStar(@PathVariable UUID id) {
        biddingService.toggleStar(id);
        return ApiResponse.success("收藏状态已更新", null);
    }
    
    @PostMapping("/items/{id}/link-project")
    @Operation(summary = "关联到项目")
    public ApiResponse<Void> linkToProject(
            @PathVariable UUID id,
            @RequestBody LinkProjectRequest request) {
        biddingService.linkToProject(id, request.projectId());
        return ApiResponse.success("已关联到项目", null);
    }
    
    // ========== 匹配与分析 ==========
    
    @GetMapping("/matched")
    @Operation(summary = "获取匹配的招标信息")
    public ApiResponse<List<BiddingItem>> getMatchedItems() {
        return ApiResponse.success(biddingService.getMatchedItems());
    }
    
    @GetMapping("/upcoming")
    @Operation(summary = "获取即将截止的招标")
    public ApiResponse<List<BiddingItem>> getUpcomingDeadlines(
            @RequestParam(defaultValue = "7") int days) {
        return ApiResponse.success(biddingService.getUpcomingDeadlines(days));
    }
    
    // ========== 统计信息 ==========
    
    @GetMapping("/statistics")
    @Operation(summary = "获取统计信息")
    public ApiResponse<Map<String, Object>> getStatistics() {
        return ApiResponse.success(biddingService.getStatistics());
    }
    
    @GetMapping("/filters/regions")
    @Operation(summary = "获取可用地区列表")
    public ApiResponse<List<String>> getRegions() {
        return ApiResponse.success(biddingService.getAvailableRegions());
    }
    
    @GetMapping("/filters/industries")
    @Operation(summary = "获取可用行业列表")
    public ApiResponse<List<String>> getIndustries() {
        return ApiResponse.success(biddingService.getAvailableIndustries());
    }
    
    // ========== 关键词配置 ==========
    
    @GetMapping("/keywords")
    @Operation(summary = "获取监控关键词")
    public ApiResponse<List<String>> getKeywords() {
        return ApiResponse.success(biddingService.getUserKeywords());
    }
    
    @PutMapping("/keywords")
    @Operation(summary = "更新监控关键词")
    public ApiResponse<Void> updateKeywords(@RequestBody UpdateKeywordsRequest request) {
        biddingService.updateUserKeywords(request.keywords());
        return ApiResponse.success("关键词已更新", null);
    }
    
    // ========== 初始化 ==========
    
    @PostMapping("/init")
    @Operation(summary = "初始化默认数据源")
    public ApiResponse<Void> initDefaultSources() {
        biddingService.initDefaultSources();
        return ApiResponse.success("初始化完成", null);
    }
    
    // ========== DTO ==========
    
    public record LinkProjectRequest(UUID projectId) {}
    
    public record UpdateKeywordsRequest(List<String> keywords) {}
}
