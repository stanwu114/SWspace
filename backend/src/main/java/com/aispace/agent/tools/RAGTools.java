package com.aispace.agent.tools;

import com.aispace.service.RAGService;
import com.aispace.service.RAGService.SearchResult;
import io.agentscope.core.tool.Tool;
import io.agentscope.core.tool.ToolParam;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * RAG 检索增强工具集 - 使用 AgentScope 官方 @Tool 注解
 * 提供语义搜索和知识上下文构建能力
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RAGTools {

    private final RAGService ragService;

    /**
     * RAG 智能检索 - 混合语义和关键词搜索
     */
    @Tool(description = "使用RAG混合检索搜索知识库，结合语义搜索和关键词搜索获取最相关的知识内容")
    public String ragSearch(
            @ToolParam(name = "query", description = "搜索查询内容") String query,
            @ToolParam(name = "topK", description = "返回结果数量，默认5") Integer topK) {
        try {
            int k = topK != null ? topK : 5;
            List<SearchResult> results = ragService.hybridSearch(query, k, 0.6);
            
            if (results.isEmpty()) {
                return "未找到与「" + query + "」相关的知识内容";
            }

            StringBuilder sb = new StringBuilder("RAG检索结果（共 " + results.size() + " 条）：\n\n");
            for (int i = 0; i < results.size(); i++) {
                SearchResult r = results.get(i);
                sb.append(String.format("【%d】%s\n", i + 1, r.getTitle()));
                sb.append(String.format("  分类: %s | 匹配方式: %s | 相关度: %.2f\n",
                    r.getCategory(), r.getMatchType(), r.getFusedScore()));
                String content = r.getSummary() != null ? r.getSummary() : r.getContent();
                if (content != null && content.length() > 300) {
                    content = content.substring(0, 300) + "...";
                }
                sb.append("  内容: ").append(content).append("\n\n");
            }
            return sb.toString();
        } catch (Exception e) {
            log.error("RAG检索失败", e);
            return "检索失败: " + e.getMessage();
        }
    }

    /**
     * 构建 RAG 增强上下文 - 为回答问题提供背景知识
     */
    @Tool(description = "根据问题构建RAG增强上下文，用于辅助回答用户问题。调用此工具获取相关背景知识后再回答用户。")
    public String buildContext(
            @ToolParam(name = "question", description = "用户的问题") String question,
            @ToolParam(name = "maxItems", description = "最大检索条目数，默认3") Integer maxItems) {
        try {
            int max = maxItems != null ? maxItems : 3;
            String context = ragService.buildRAGContext(question, max);
            
            if (context.isEmpty()) {
                return "未找到与该问题相关的知识库内容，请根据已有知识回答。";
            }
            
            return context;
        } catch (Exception e) {
            log.error("构建RAG上下文失败", e);
            return "上下文构建失败: " + e.getMessage();
        }
    }

    /**
     * 获取相关知识推荐
     */
    @Tool(description = "根据知识条目ID获取相关知识推荐")
    public String getRelatedKnowledge(
            @ToolParam(name = "knowledgeId", description = "知识条目ID (UUID格式)") String knowledgeId,
            @ToolParam(name = "topK", description = "推荐数量，默认3") Integer topK) {
        try {
            java.util.UUID id = java.util.UUID.fromString(knowledgeId);
            int k = topK != null ? topK : 3;
            List<SearchResult> results = ragService.getRelatedKnowledge(id, k);
            
            if (results.isEmpty()) {
                return "未找到相关推荐知识";
            }

            StringBuilder sb = new StringBuilder("相关知识推荐：\n");
            for (SearchResult r : results) {
                sb.append(String.format("- 【%s】%s (分类: %s)\n", r.getId(), r.getTitle(), r.getCategory()));
            }
            return sb.toString();
        } catch (IllegalArgumentException e) {
            return "无效的知识ID格式: " + knowledgeId;
        } catch (Exception e) {
            log.error("获取相关推荐失败", e);
            return "获取失败: " + e.getMessage();
        }
    }
}
