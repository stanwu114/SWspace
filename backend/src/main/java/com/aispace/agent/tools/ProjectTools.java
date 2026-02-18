package com.aispace.agent.tools;

import com.aispace.dto.response.PageResponse;
import com.aispace.entity.Project;
import com.aispace.service.ProjectService;
import io.agentscope.core.tool.Tool;
import io.agentscope.core.tool.ToolParam;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * 项目管理工具集 - 使用 AgentScope 官方 @Tool 注解
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ProjectTools {

    private final ProjectService projectService;

    /**
     * 查询项目列表
     */
    @Tool(description = "获取项目列表，支持按状态筛选")
    public String listProjects(
            @ToolParam(name = "status", description = "项目状态筛选: LEAD, OPPORTUNITY, EXECUTION, COMPLETED, CANCELLED") String status,
            @ToolParam(name = "keyword", description = "搜索关键词") String keyword) {
        try {
            Project.ProjectStatus projectStatus = status != null && !status.isEmpty()
                ? Project.ProjectStatus.valueOf(status) : null;
            
            PageResponse<Project> result = projectService.listProjects(
                projectStatus, null, keyword,
                1, 20, "createdAt", "desc"
            );
            
            if (result.getItems().isEmpty()) {
                return "未找到项目记录";
            }

            StringBuilder sb = new StringBuilder("项目列表：\n");
            for (Project p : result.getItems()) {
                sb.append(String.format("- ID: %s, 名称: %s, 状态: %s, 预估金额: %s\n",
                    p.getId(), p.getName(), p.getStatus(), 
                    p.getEstimatedValue() != null ? p.getEstimatedValue() : "未设置"));
            }
            return sb.toString();
        } catch (Exception e) {
            log.error("查询项目列表失败", e);
            return "查询失败: " + e.getMessage();
        }
    }

    /**
     * 查询项目详情
     */
    @Tool(description = "获取指定项目的详细信息")
    public String getProjectDetail(
            @ToolParam(name = "projectId", description = "项目ID") String projectId) {
        try {
            return projectService.getProject(UUID.fromString(projectId))
                .map(project -> String.format("""
                    项目详情：
                    - ID: %s
                    - 名称: %s
                    - 编号: %s
                    - 状态: %s
                    - 阶段: %s
                    - 预估金额: %s
                    - 合同金额: %s
                    - 投标截止: %s
                    - 描述: %s
                    - 创建时间: %s
                    """,
                    project.getId(),
                    project.getName(),
                    project.getCode(),
                    project.getStatus(),
                    project.getStage(),
                    project.getEstimatedValue(),
                    project.getContractValue(),
                    formatDate(project.getBidDeadline()),
                    truncate(project.getDescription(), 200),
                    project.getCreatedAt()
                ))
                .orElse("未找到项目 ID: " + projectId);
        } catch (Exception e) {
            log.error("查询项目详情失败", e);
            return "查询失败: " + e.getMessage();
        }
    }

    /**
     * 创建项目
     */
    @Tool(description = "创建新项目")
    public String createProject(
            @ToolParam(name = "name", description = "项目名称") String name,
            @ToolParam(name = "customerId", description = "关联客户ID") String customerId,
            @ToolParam(name = "estimatedValue", description = "预估金额") Double estimatedValue,
            @ToolParam(name = "description", description = "项目描述") String description) {
        try {
            Project project = Project.builder()
                .name(name)
                .customerId(customerId != null && !customerId.isEmpty() 
                    ? UUID.fromString(customerId) : null)
                .estimatedValue(estimatedValue != null 
                    ? BigDecimal.valueOf(estimatedValue) : null)
                .description(description)
                .status(Project.ProjectStatus.LEAD)
                .build();
            
            Project saved = projectService.createProject(project);
            return "项目创建成功，ID: " + saved.getId();
        } catch (Exception e) {
            log.error("创建项目失败", e);
            return "创建失败: " + e.getMessage();
        }
    }

    /**
     * 更新项目状态
     */
    @Tool(description = "更新项目状态")
    public String updateProjectStatus(
            @ToolParam(name = "projectId", description = "项目ID") String projectId,
            @ToolParam(name = "status", description = "新状态: LEAD, OPPORTUNITY, EXECUTION, COMPLETED, CANCELLED") String status) {
        try {
            projectService.updateStatus(
                UUID.fromString(projectId),
                Project.ProjectStatus.valueOf(status),
                null
            );
            return "项目状态更新成功";
        } catch (Exception e) {
            log.error("更新项目状态失败", e);
            return "更新失败: " + e.getMessage();
        }
    }

    /**
     * 获取客户的项目
     */
    @Tool(description = "获取指定客户的所有项目")
    public String getCustomerProjects(
            @ToolParam(name = "customerId", description = "客户ID") String customerId) {
        try {
            var projects = projectService.getProjectsByCustomer(UUID.fromString(customerId));
            if (projects.isEmpty()) {
                return "该客户暂无项目";
            }
            
            StringBuilder sb = new StringBuilder("客户项目列表：\n");
            for (Project p : projects) {
                sb.append(String.format("- %s (ID: %s), 状态: %s, 预估: %s\n",
                    p.getName(), p.getId(), p.getStatus(), 
                    p.getEstimatedValue() != null ? p.getEstimatedValue() : "未设置"));
            }
            return sb.toString();
        } catch (Exception e) {
            log.error("获取客户项目失败", e);
            return "查询失败: " + e.getMessage();
        }
    }

    /**
     * 获取即将到期的项目
     */
    @Tool(description = "获取即将到投标截止日期的项目")
    public String getUpcomingDeadlines(
            @ToolParam(name = "limit", description = "返回数量限制，默认10") Integer limit) {
        try {
            var projects = projectService.getUpcomingDeadlines(limit != null ? limit : 10);
            if (projects.isEmpty()) {
                return "暂无即将到期的项目";
            }
            
            StringBuilder sb = new StringBuilder("即将到期的项目：\n");
            for (Project p : projects) {
                sb.append(String.format("- %s (ID: %s), 截止: %s\n",
                    p.getName(), p.getId(), formatDate(p.getBidDeadline())));
            }
            return sb.toString();
        } catch (Exception e) {
            log.error("获取到期项目失败", e);
            return "查询失败: " + e.getMessage();
        }
    }

    private String formatDate(LocalDateTime date) {
        if (date == null) return "未设置";
        return date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
    }

    private String truncate(String text, int maxLength) {
        if (text == null) return "无";
        if (text.length() <= maxLength) return text;
        return text.substring(0, maxLength) + "...";
    }
}
