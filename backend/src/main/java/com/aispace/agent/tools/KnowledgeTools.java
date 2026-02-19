package com.aispace.agent.tools;

import com.aispace.dto.response.PageResponse;
import com.aispace.entity.Knowledge;
import com.aispace.service.KnowledgeService;
import io.agentscope.core.tool.Tool;
import io.agentscope.core.tool.ToolParam;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * 知识库管理工具集 - 使用 AgentScope 官方 @Tool 注解
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class KnowledgeTools {

    private final KnowledgeService knowledgeService;

    /**
     * 搜索知识库
     */
    @Tool(description = "搜索知识库内容")
    public String searchKnowledge(
            @ToolParam(name = "keyword", description = "搜索关键词") String keyword,
            @ToolParam(name = "category", description = "分类: INDUSTRY, SOLUTION, CASE, SALES, LESSON, TEMPLATE, OTHER") String category,
            @ToolParam(name = "limit", description = "返回结果数量限制，默认5条") Integer limit) {
        try {
            int resultLimit = limit != null ? limit : 5;
            
            Knowledge.KnowledgeCategory cat = category != null && !category.isEmpty()
                ? Knowledge.KnowledgeCategory.valueOf(category) : null;
            
            PageResponse<Knowledge> result = knowledgeService.listKnowledge(
                cat, null, keyword,
                1, resultLimit, "createdAt", "desc"
            );
            
            if (result.getItems().isEmpty()) {
                return "未找到相关知识内容";
            }

            StringBuilder sb = new StringBuilder("知识库搜索结果：\n");
            for (Knowledge k : result.getItems()) {
                sb.append(String.format(
                    "【%s】%s\n分类: %s | 标签: %s\n摘要: %s\n---\n",
                    k.getId(),
                    k.getTitle(),
                    k.getCategory(),
                    k.getTags(),
                    truncate(k.getSummary() != null ? k.getSummary() : k.getContent(), 200)
                ));
            }
            return sb.toString();
        } catch (Exception e) {
            log.error("搜索知识库失败", e);
            return "搜索失败: " + e.getMessage();
        }
    }

    /**
     * 查询知识详情
     */
    @Tool(description = "获取知识条目详情")
    public String getKnowledgeDetail(
            @ToolParam(name = "knowledgeId", description = "知识条目ID") String knowledgeId) {
        try {
            var knowledgeOpt = knowledgeService.getKnowledge(UUID.fromString(knowledgeId), true);
            if (knowledgeOpt.isEmpty()) {
                return "未找到知识条目 ID: " + knowledgeId;
            }
            Knowledge k = knowledgeOpt.get();
            return String.format(
                "知识详情：\n- ID: %s\n- 标题: %s\n- 分类: %s\n- 子分类: %s\n- 标签: %s\n- 内容:\n%s\n- 来源: %s\n- 浏览次数: %d\n- 创建时间: %s\n",
                k.getId(),
                k.getTitle(),
                k.getCategory(),
                k.getSubcategory(),
                k.getTags(),
                k.getContent(),
                k.getSourceName() != null ? k.getSourceName() : "未知",
                k.getViewCount(),
                k.getCreatedAt()
            );
        } catch (Exception e) {
            log.error("查询知识详情失败", e);
            return "查询失败: " + e.getMessage();
        }
    }

    /**
     * 添加知识条目
     */
    @Tool(description = "添加新知识条目")
    public String addKnowledge(
            @ToolParam(name = "title", description = "标题") String title,
            @ToolParam(name = "content", description = "内容") String content,
            @ToolParam(name = "category", description = "分类: INDUSTRY, SOLUTION, CASE, SALES, LESSON, TEMPLATE, OTHER") String category,
            @ToolParam(name = "tags", description = "标签，多个用逗号分隔") String tags) {
        try {
            Knowledge knowledge = Knowledge.builder()
                .title(title)
                .content(content)
                .category(category != null && !category.isEmpty()
                    ? Knowledge.KnowledgeCategory.valueOf(category)
                    : Knowledge.KnowledgeCategory.OTHER)
                .tags(tags != null ? java.util.Arrays.asList(tags.split(",")) : java.util.List.of())
                .sourceType(Knowledge.KnowledgeSourceType.MANUAL)
                .sourceName("AI助手录入")
                .build();
            
            Knowledge saved = knowledgeService.createKnowledge(knowledge);
            return "知识条目添加成功，ID: " + saved.getId();
        } catch (Exception e) {
            log.error("添加知识条目失败", e);
            return "添加失败: " + e.getMessage();
        }
    }

    /**
     * 获取知识分类列表
     */
    @Tool(description = "获取知识库分类统计")
    public String getCategoryStats() {
        try {
            var stats = knowledgeService.getCategoryStats();
            if (stats.isEmpty()) {
                return "暂无知识分类";
            }
            
            StringBuilder sb = new StringBuilder("知识分类统计：\n");
            stats.forEach((cat, count) -> {
                sb.append(String.format("- %s: %d 条\n", cat, count));
            });
            return sb.toString();
        } catch (Exception e) {
            log.error("获取知识分类失败", e);
            return "获取失败: " + e.getMessage();
        }
    }

    /**
     * 获取精选知识
     */
    @Tool(description = "获取精选知识内容")
    public String getFeaturedKnowledge(
            @ToolParam(name = "limit", description = "返回数量限制") Integer limit) {
        try {
            var items = knowledgeService.getFeaturedKnowledge(limit != null ? limit : 5);
            if (items.isEmpty()) {
                return "暂无精选知识";
            }
            
            StringBuilder sb = new StringBuilder("精选知识：\n");
            for (Knowledge k : items) {
                sb.append(String.format("- 【%s】%s (浏览: %d)\n",
                    k.getId(), k.getTitle(), k.getViewCount()));
            }
            return sb.toString();
        } catch (Exception e) {
            log.error("获取精选知识失败", e);
            return "获取失败: " + e.getMessage();
        }
    }

    /**
     * 删除知识条目
     */
    @Tool(description = "删除知识条目")
    public String deleteKnowledge(
            @ToolParam(name = "knowledgeId", description = "知识条目ID") String knowledgeId) {
        try {
            knowledgeService.deleteKnowledge(UUID.fromString(knowledgeId));
            return "知识条目删除成功";
        } catch (Exception e) {
            log.error("删除知识条目失败", e);
            return "删除失败: " + e.getMessage();
        }
    }

    private String truncate(String text, int maxLength) {
        if (text == null) return "";
        if (text.length() <= maxLength) return text;
        return text.substring(0, maxLength) + "...";
    }
}
