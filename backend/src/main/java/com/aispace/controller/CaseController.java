package com.aispace.controller;

import com.aispace.dto.response.ApiResponse;
import com.aispace.dto.response.PageResponse;
import com.aispace.entity.Case;
import com.aispace.entity.Knowledge;
import com.aispace.service.CaseService;
import com.aispace.service.RAGService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 案例管理控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/cases")
@RequiredArgsConstructor
@Tag(name = "案例管理", description = "项目案例的管理和复用")
public class CaseController {
    
    private final CaseService caseService;
    private final RAGService ragService;
    
    @GetMapping
    @Operation(summary = "获取案例列表")
    public ApiResponse<PageResponse<Case>> listCases(
            @RequestParam(required = false) String industry,
            @RequestParam(required = false) String region,
            @RequestParam(required = false) Case.CustomerType customerType,
            @RequestParam(required = false) Case.CaseStatus status,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        
        PageResponse<Case> result = caseService.listCases(
            industry, region, customerType, status, keyword, page, pageSize);
        return ApiResponse.success(result);
    }
    
    @GetMapping("/{id}")
    @Operation(summary = "获取案例详情")
    public ApiResponse<Case> getCase(@PathVariable UUID id) {
        return caseService.getCase(id, true)
            .map(ApiResponse::success)
            .orElse(ApiResponse.notFound("案例不存在"));
    }
    
    @PostMapping
    @Operation(summary = "创建案例")
    public ApiResponse<Case> createCase(@RequestBody Case caseEntity) {
        return ApiResponse.success("案例创建成功", caseService.createCase(caseEntity));
    }
    
    @PutMapping("/{id}")
    @Operation(summary = "更新案例")
    public ApiResponse<Case> updateCase(@PathVariable UUID id, @RequestBody Case caseEntity) {
        return ApiResponse.success("案例更新成功", caseService.updateCase(id, caseEntity));
    }
    
    @DeleteMapping("/{id}")
    @Operation(summary = "删除案例")
    public ApiResponse<Void> deleteCase(@PathVariable UUID id) {
        caseService.deleteCase(id);
        return ApiResponse.success("案例已删除", null);
    }
    
    @PostMapping("/{id}/publish")
    @Operation(summary = "发布案例")
    public ApiResponse<Case> publishCase(@PathVariable UUID id) {
        return ApiResponse.success("案例已发布", caseService.publishCase(id));
    }
    
    @PostMapping("/{id}/archive")
    @Operation(summary = "归档案例")
    public ApiResponse<Case> archiveCase(@PathVariable UUID id) {
        return ApiResponse.success("案例已归档", caseService.archiveCase(id));
    }
    
    @PostMapping("/{id}/toggle-featured")
    @Operation(summary = "切换精选状态")
    public ApiResponse<Case> toggleFeatured(@PathVariable UUID id) {
        return ApiResponse.success(caseService.toggleFeatured(id));
    }
    
    @GetMapping("/featured")
    @Operation(summary = "获取精选案例")
    public ApiResponse<List<Case>> getFeaturedCases(
            @RequestParam(defaultValue = "6") int limit) {
        return ApiResponse.success(caseService.getFeaturedCases(limit));
    }
    
    @PostMapping("/{id}/reference")
    @Operation(summary = "记录案例引用")
    public ApiResponse<Void> recordReference(@PathVariable UUID id) {
        caseService.recordReference(id);
        return ApiResponse.success("已记录引用", null);
    }
    
    @PostMapping("/from-project/{projectId}")
    @Operation(summary = "从项目创建案例")
    public ApiResponse<Case> createFromProject(
            @PathVariable UUID projectId,
            @RequestParam(required = false) String author) {
        return ApiResponse.success("案例创建成功", caseService.createFromProject(projectId, author));
    }
    
    @GetMapping("/by-project/{projectId}")
    @Operation(summary = "获取项目关联的案例")
    public ApiResponse<List<Case>> getCasesByProject(@PathVariable UUID projectId) {
        return ApiResponse.success(caseService.getCasesByProject(projectId));
    }
    
    @PostMapping("/{id}/extract-knowledge")
    @Operation(summary = "从案例提取知识")
    public ApiResponse<List<Knowledge>> extractKnowledge(@PathVariable UUID id) {
        Case caseEntity = caseService.getCase(id, false)
            .orElseThrow(() -> new RuntimeException("Case not found: " + id));
        List<Knowledge> extracted = caseService.extractKnowledgeFromCase(caseEntity);
        return ApiResponse.success("知识提取完成", extracted);
    }
    
    @GetMapping("/statistics")
    @Operation(summary = "获取案例统计")
    public ApiResponse<Map<String, Object>> getStatistics() {
        return ApiResponse.success(caseService.getStatistics());
    }
    
    @GetMapping("/filters")
    @Operation(summary = "获取筛选选项")
    public ApiResponse<Map<String, List<String>>> getFilterOptions() {
        return ApiResponse.success(caseService.getFilterOptions());
    }
    
    // ========== RAG相关接口 ==========
    
    @PostMapping("/search/semantic")
    @Operation(summary = "语义搜索案例")
    public ApiResponse<List<RAGService.SearchResult>> semanticSearch(
            @RequestBody SemanticSearchRequest request) {
        List<RAGService.SearchResult> results = ragService.semanticSearch(
            request.query(),
            request.topK() > 0 ? request.topK() : 5,
            request.minScore() > 0 ? request.minScore() : 0.6
        );
        return ApiResponse.success(results);
    }
    
    @PostMapping("/search/hybrid")
    @Operation(summary = "混合搜索知识库")
    public ApiResponse<List<RAGService.SearchResult>> hybridSearch(
            @RequestBody SemanticSearchRequest request) {
        List<RAGService.SearchResult> results = ragService.hybridSearch(
            request.query(),
            request.topK() > 0 ? request.topK() : 5,
            request.minScore() > 0 ? request.minScore() : 0.6
        );
        return ApiResponse.success(results);
    }
    
    @GetMapping("/{id}/related")
    @Operation(summary = "获取相关知识")
    public ApiResponse<List<RAGService.SearchResult>> getRelatedKnowledge(
            @PathVariable UUID id,
            @RequestParam(defaultValue = "5") int topK) {
        return ApiResponse.success(ragService.getRelatedKnowledge(id, topK));
    }
    
    @PostMapping("/ask")
    @Operation(summary = "知识问答（RAG）")
    public ApiResponse<String> askWithRAG(@RequestBody AskRequest request) {
        String answer = ragService.askWithRAG(request.question());
        return ApiResponse.success(answer);
    }
    
    // ========== DTO ==========
    
    public record SemanticSearchRequest(String query, int topK, double minScore) {}
    
    public record AskRequest(String question) {}
}
