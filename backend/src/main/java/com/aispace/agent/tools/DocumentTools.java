package com.aispace.agent.tools;

import com.aispace.entity.Document;
import com.aispace.service.DocumentService;
import io.agentscope.core.tool.Tool;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 文档工具类 - 供 DocAgent 使用
 * 实现文档管理、内容分析、模板生成等功能
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DocumentTools {

    private final DocumentService documentService;

    /**
     * 查询文档列表
     */
    @Tool(description = "查询文档列表，支持按项目ID、类型筛选")
    public List<Document> listDocuments(String projectId, String type, int limit) {
        log.info("查询文档列表: projectId={}, type={}", projectId, type);
        try {
            if (projectId != null && !projectId.isBlank()) {
                return documentService.getProjectDocuments(UUID.fromString(projectId));
            }
            // 返回最近的文档
            var result = documentService.listDocuments(null, null, null, 1, limit);
            return result.getItems();
        } catch (Exception e) {
            log.error("查询文档列表失败", e);
            return List.of();
        }
    }

    /**
     * 获取文档详情
     */
    @Tool(description = "获取文档的详细信息")
    public Document getDocumentDetail(String documentId) {
        log.info("获取文档详情: id={}", documentId);
        try {
            return documentService.getDocument(UUID.fromString(documentId)).orElse(null);
        } catch (Exception e) {
            log.error("获取文档详情失败", e);
            return null;
        }
    }

    /**
     * 分析文档内容
     */
    @Tool(description = "分析文档内容，提取关键信息和结构")
    public Map<String, Object> analyzeDocumentContent(String content, String docType) {
        log.info("分析文档内容: type={}, length={}", docType, content != null ? content.length() : 0);
        Map<String, Object> analysis = new HashMap<>();
        
        if (content == null || content.isBlank()) {
            analysis.put("error", "文档内容为空");
            return analysis;
        }
        
        analysis.put("contentLength", content.length());
        analysis.put("docType", docType);
        analysis.put("wordCount", content.split("\\s+").length);
        analysis.put("structure", analyzeStructure(content, docType));
        analysis.put("keyPoints", extractKeyPoints(content, docType));
        analysis.put("suggestions", generateSuggestions(content, docType));
        
        return analysis;
    }

    /**
     * 生成文档大纲
     */
    @Tool(description = "根据文档类型生成标准大纲模板")
    public Map<String, Object> generateOutline(String docType, String topic) {
        log.info("生成文档大纲: type={}, topic={}", docType, topic);
        Map<String, Object> outline = new HashMap<>();
        
        outline.put("docType", docType);
        outline.put("topic", topic);
        outline.put("sections", getOutlineSections(docType, topic));
        outline.put("estimatedLength", estimateLength(docType));
        
        return outline;
    }

    /**
     * 检查文档完整性
     */
    @Tool(description = "检查文档是否包含必需的章节和内容")
    public Map<String, Object> checkCompleteness(String content, String docType) {
        log.info("检查文档完整性: type={}", docType);
        Map<String, Object> result = new HashMap<>();
        
        result.put("docType", docType);
        result.put("isComplete", true);
        
        List<String> requiredSections = getRequiredSections(docType);
        List<String> missingSections = new java.util.ArrayList<>();
        
        for (String section : requiredSections) {
            if (!content.contains(section)) {
                missingSections.add(section);
            }
        }
        
        result.put("missingSections", missingSections);
        result.put("completenessScore", (requiredSections.size() - missingSections.size()) * 100 / requiredSections.size());
        
        if (!missingSections.isEmpty()) {
            result.put("isComplete", false);
            result.put("suggestion", "建议补充以下章节: " + String.join(", ", missingSections));
        }
        
        return result;
    }

    /**
     * 提取文档摘要
     */
    @Tool(description = "提取文档的摘要内容")
    public Map<String, Object> extractSummary(String content, int maxLength) {
        log.info("提取文档摘要: maxLength={}", maxLength);
        Map<String, Object> summary = new HashMap<>();
        
        if (content == null || content.isBlank()) {
            summary.put("error", "内容为空");
            return summary;
        }
        
        // 简单摘要提取（实际应由 LLM 完成）
        String extracted = content.length() > maxLength 
            ? content.substring(0, maxLength) + "..." 
            : content;
        
        summary.put("summary", extracted);
        summary.put("originalLength", content.length());
        summary.put("summaryLength", extracted.length());
        
        return summary;
    }

    /**
     * 比较文档差异
     */
    @Tool(description = "比较两个文档的差异")
    public Map<String, Object> compareDocuments(String content1, String content2) {
        log.info("比较文档差异");
        Map<String, Object> comparison = new HashMap<>();
        
        comparison.put("doc1Length", content1 != null ? content1.length() : 0);
        comparison.put("doc2Length", content2 != null ? content2.length() : 0);
        comparison.put("lengthDiff", Math.abs((content1 != null ? content1.length() : 0) - (content2 != null ? content2.length() : 0)));
        comparison.put("similarity", calculateSimilarity(content1, content2));
        
        return comparison;
    }

    // ==================== 私有辅助方法 ====================

    private Map<String, Object> analyzeStructure(String content, String docType) {
        Map<String, Object> structure = new HashMap<>();
        
        // 检测章节标题
        int h1Count = countMatches(content, "# ");
        int h2Count = countMatches(content, "## ");
        int h3Count = countMatches(content, "### ");
        
        structure.put("h1Count", h1Count);
        structure.put("h2Count", h2Count);
        structure.put("h3Count", h3Count);
        structure.put("hasTableOfContents", content.contains("目录") || content.contains("## 目录"));
        
        return structure;
    }

    private List<String> extractKeyPoints(String content, String docType) {
        // 简单关键点提取
        java.util.List<String> points = new java.util.ArrayList<>();
        
        if (content.contains("目标") || content.contains("目的")) {
            points.add("包含目标/目的描述");
        }
        if (content.contains("方案") || content.contains("设计")) {
            points.add("包含方案设计");
        }
        if (content.contains("预算") || content.contains("报价")) {
            points.add("包含预算/报价信息");
        }
        if (content.contains("时间") || content.contains("进度")) {
            points.add("包含时间/进度安排");
        }
        
        return points;
    }

    private List<String> generateSuggestions(String content, String docType) {
        java.util.List<String> suggestions = new java.util.ArrayList<>();
        
        if (content.length() < 500) {
            suggestions.add("文档内容较短，建议补充更多细节");
        }
        if (!content.contains("##")) {
            suggestions.add("建议使用标题层级组织文档结构");
        }
        if (docType != null && docType.equals("SOLUTION") && !content.contains("架构")) {
            suggestions.add("技术方案建议包含架构设计章节");
        }
        
        return suggestions;
    }

    private List<String> getOutlineSections(String docType, String topic) {
        return switch (docType.toUpperCase()) {
            case "SOLUTION", "BID" -> List.of(
                "1. 项目概述",
                "   1.1 项目背景",
                "   1.2 建设目标",
                "   1.3 建设原则",
                "2. 需求分析",
                "   2.1 业务需求",
                "   2.2 功能需求",
                "   2.3 非功能需求",
                "3. 总体设计",
                "   3.1 设计思路",
                "   3.2 总体架构",
                "   3.3 技术路线",
                "4. 详细设计",
                "5. 实施方案",
                "6. 保障措施"
            );
            case "REPORT" -> List.of(
                "1. 概述",
                "2. 现状分析",
                "3. 问题诊断",
                "4. 解决方案",
                "5. 效果评估",
                "6. 总结建议"
            );
            default -> List.of(
                "1. 概述",
                "2. 主要内容",
                "3. 分析与讨论",
                "4. 结论"
            );
        };
    }

    private int estimateLength(String docType) {
        return switch (docType.toUpperCase()) {
            case "SOLUTION", "BID" -> 10000;
            case "REPORT" -> 5000;
            default -> 3000;
        };
    }

    private List<String> getRequiredSections(String docType) {
        return switch (docType.toUpperCase()) {
            case "SOLUTION", "BID" -> List.of("项目概述", "需求分析", "总体设计", "实施方案");
            case "REPORT" -> List.of("概述", "分析", "结论");
            default -> List.of("概述", "结论");
        };
    }

    private int countMatches(String content, String pattern) {
        int count = 0;
        int index = 0;
        while ((index = content.indexOf(pattern, index)) != -1) {
            count++;
            index += pattern.length();
        }
        return count;
    }

    private double calculateSimilarity(String content1, String content2) {
        if (content1 == null || content2 == null) return 0;
        
        // 简单相似度计算（实际应使用更复杂的算法）
        String[] words1 = content1.split("\\s+");
        String[] words2 = content2.split("\\s+");
        
        java.util.Set<String> set1 = new java.util.HashSet<>(java.util.Arrays.asList(words1));
        java.util.Set<String> set2 = new java.util.HashSet<>(java.util.Arrays.asList(words2));
        
        java.util.Set<String> intersection = new java.util.HashSet<>(set1);
        intersection.retainAll(set2);
        
        java.util.Set<String> union = new java.util.HashSet<>(set1);
        union.addAll(set2);
        
        return union.isEmpty() ? 0 : (double) intersection.size() / union.size();
    }
}
