package com.aispace.controller;

import com.aispace.dto.response.ApiResponse;
import com.aispace.dto.response.PageResponse;
import com.aispace.entity.Project;
import com.aispace.service.ProjectService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * 项目管理控制器
 */
@RestController
@RequestMapping("/api/v1/projects")
@RequiredArgsConstructor
@Tag(name = "项目管理", description = "项目CRUD和状态管理接口")
public class ProjectController {
    
    private final ProjectService projectService;
    
    /**
     * 获取项目列表
     */
    @GetMapping
    @Operation(summary = "获取项目列表")
    public ApiResponse<PageResponse<Project>> listProjects(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) UUID customerId,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) String sort,
            @RequestParam(defaultValue = "desc") String order
    ) {
        Project.ProjectStatus statusEnum = status != null 
            ? Project.ProjectStatus.valueOf(status.toUpperCase()) 
            : null;
        
        PageResponse<Project> result = projectService.listProjects(
            statusEnum, customerId, keyword, page, pageSize, sort, order
        );
        
        return ApiResponse.success(result);
    }
    
    /**
     * 获取项目详情
     */
    @GetMapping("/{id}")
    @Operation(summary = "获取项目详情")
    public ApiResponse<Project> getProject(@PathVariable UUID id) {
        return projectService.getProject(id)
            .map(ApiResponse::success)
            .orElse(ApiResponse.notFound("项目不存在"));
    }
    
    /**
     * 创建项目
     */
    @PostMapping
    @Operation(summary = "创建项目")
    public ApiResponse<Project> createProject(@RequestBody Project project) {
        Project created = projectService.createProject(project);
        return ApiResponse.success("项目创建成功", created);
    }
    
    /**
     * 更新项目
     */
    @PutMapping("/{id}")
    @Operation(summary = "更新项目")
    public ApiResponse<Project> updateProject(
            @PathVariable UUID id,
            @RequestBody Project project
    ) {
        Project updated = projectService.updateProject(id, project);
        return ApiResponse.success("项目更新成功", updated);
    }
    
    /**
     * 更新项目状态
     */
    @PatchMapping("/{id}/status")
    @Operation(summary = "更新项目状态")
    public ApiResponse<Project> updateStatus(
            @PathVariable UUID id,
            @RequestBody StatusUpdateRequest request
    ) {
        Project.ProjectStatus status = Project.ProjectStatus.valueOf(
            request.status().toUpperCase()
        );
        Project updated = projectService.updateStatus(id, status, request.note());
        return ApiResponse.success("状态更新成功", updated);
    }
    
    /**
     * 删除项目
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "删除项目")
    public ApiResponse<Void> deleteProject(@PathVariable UUID id) {
        projectService.deleteProject(id);
        return ApiResponse.success("项目删除成功", null);
    }
    
    /**
     * 状态更新请求
     */
    public record StatusUpdateRequest(String status, String note) {}
}
