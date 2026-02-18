package com.aispace.controller;

import com.aispace.dto.response.ApiResponse;
import com.aispace.dto.response.PageResponse;
import com.aispace.entity.Knowledge;
import com.aispace.service.KnowledgeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 知识库控制器
 */
@RestController
@RequestMapping("/api/v1/knowledge")
@RequiredArgsConstructor
@Tag(name = "知识库", description = "知识管理和语义搜索接口")
public class KnowledgeController {
    
    private final KnowledgeService knowledgeService;
    
    /**
     * 获取知识列表
     */
    @GetMapping
    @Operation(summary = "获取知识列表")
    public ApiResponse<PageResponse<Knowledge>> listKnowledge(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String subcategory,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) String sort,
            @RequestParam(defaultValue = "desc") String order
    ) {
        Knowledge.KnowledgeCategory categoryEnum = category != null 
            ? Knowledge.KnowledgeCategory.valueOf(category.toUpperCase()) 
            : null;
        
        PageResponse<Knowledge> result = knowledgeService.listKnowledge(
            categoryEnum, subcategory, keyword, page, pageSize, sort, order
        );
        
        return ApiResponse.success(result);
    }
    
    /**
     * 获取知识详情
     */
    @GetMapping("/{id}")
    @Operation(summary = "获取知识详情")
    public ApiResponse<Knowledge> getKnowledge(
            @PathVariable UUID id,
            @RequestParam(defaultValue = "true") boolean recordView
    ) {
        return knowledgeService.getKnowledge(id, recordView)
            .map(ApiResponse::success)
            .orElse(ApiResponse.notFound("知识不存在"));
    }
    
    /**
     * 创建知识
     */
    @PostMapping
    @Operation(summary = "创建知识")
    public ApiResponse<Knowledge> createKnowledge(@RequestBody Knowledge knowledge) {
        Knowledge created = knowledgeService.createKnowledge(knowledge);
        return ApiResponse.success("知识创建成功", created);
    }
    
    /**
     * 更新知识
     */
    @PutMapping("/{id}")
    @Operation(summary = "更新知识")
    public ApiResponse<Knowledge> updateKnowledge(
            @PathVariable UUID id,
            @RequestBody Knowledge knowledge
    ) {
        Knowledge updated = knowledgeService.updateKnowledge(id, knowledge);
        return ApiResponse.success("知识更新成功", updated);
    }
    
    /**
     * 删除知识
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "删除知识")
    public ApiResponse<Void> deleteKnowledge(@PathVariable UUID id) {
        knowledgeService.deleteKnowledge(id);
        return ApiResponse.success("知识删除成功", null);
    }
    
    /**
     * 获取精选知识
     */
    @GetMapping("/featured")
    @Operation(summary = "获取精选知识")
    public ApiResponse<List<Knowledge>> getFeaturedKnowledge(
            @RequestParam(defaultValue = "10") int limit
    ) {
        List<Knowledge> featured = knowledgeService.getFeaturedKnowledge(limit);
        return ApiResponse.success(featured);
    }
    
    /**
     * 获取分类统计
     */
    @GetMapping("/stats/categories")
    @Operation(summary = "获取分类统计")
    public ApiResponse<Map<String, Long>> getCategoryStats() {
        Map<String, Long> stats = knowledgeService.getCategoryStats();
        return ApiResponse.success(stats);
    }
    
    /**
     * 语义搜索（占位，需要向量数据库支持）
     */
    @PostMapping("/semantic-search")
    @Operation(summary = "语义搜索")
    public ApiResponse<PageResponse<Knowledge>> semanticSearch(
            @RequestBody SemanticSearchRequest request
    ) {
        // TODO: 实现向量搜索
        // 暂时使用关键词搜索代替
        PageResponse<Knowledge> result = knowledgeService.listKnowledge(
            null, null, request.query(), 1, request.limit() != null ? request.limit() : 10, 
            "viewCount", "desc"
        );
        return ApiResponse.success(result);
    }
    
    /**
     * 语义搜索请求
     */
    public record SemanticSearchRequest(
        String query,
        List<String> categories,
        Integer limit,
        Double minScore
    ) {}
}
