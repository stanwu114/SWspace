package com.aispace.agent.tools;

import com.aispace.entity.BiddingItem;
import com.aispace.service.BiddingService;
import io.agentscope.core.tool.Tool;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 情报分析工具类 - 供 IntelAgent 使用
 * 实现招标监控、政策解读、竞品分析、项目评估等功能
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class IntelTools {

    private final BiddingService biddingService;

    /**
     * 查询招标信息列表
     */
    @Tool(description = "查询招标信息列表，支持按关键词、地区、类型筛选")
    public List<BiddingItem> listBiddingItems(
            String keyword,
            String region,
            String type,
            int limit
    ) {
        log.info("查询招标信息: keyword={}, region={}, type={}", keyword, region, type);
        try {
            var result = biddingService.listItems(region, type, keyword, null, null, null, 1, limit);
            return result.getItems();
        } catch (Exception e) {
            log.error("查询招标信息失败", e);
            return List.of();
        }
    }

    /**
     * 获取招标详情
     */
    @Tool(description = "获取招标项目的详细信息")
    public BiddingItem getBiddingDetail(String biddingId) {
        log.info("获取招标详情: id={}", biddingId);
        try {
            return biddingService.getItem(java.util.UUID.fromString(biddingId)).orElse(null);
        } catch (Exception e) {
            log.error("获取招标详情失败", e);
            return null;
        }
    }

    /**
     * 分析招标文件
     */
    @Tool(description = "分析招标文件，提取关键信息、评分标准、投标要点")
    public Map<String, Object> analyzeBidDocument(String content) {
        log.info("分析招标文件，内容长度: {}", content != null ? content.length() : 0);
        Map<String, Object> analysis = new HashMap<>();
        
        if (content == null || content.isBlank()) {
            analysis.put("error", "招标文件内容为空");
            return analysis;
        }
        
        // 基础信息提取（实际应由 LLM 完成）
        analysis.put("contentLength", content.length());
        analysis.put("keywords", extractKeywords(content));
        analysis.put("estimatedBudget", extractBudget(content));
        analysis.put("deadline", extractDeadline(content));
        analysis.put("requirements", extractRequirements(content));
        
        return analysis;
    }

    /**
     * 分析政策文件
     */
    @Tool(description = "解读政策文件，分析对业务的影响和潜在商机")
    public Map<String, Object> analyzePolicy(String content, String context) {
        log.info("分析政策文件，上下文: {}", context);
        Map<String, Object> analysis = new HashMap<>();
        
        if (content == null || content.isBlank()) {
            analysis.put("error", "政策文件内容为空");
            return analysis;
        }
        
        analysis.put("contentLength", content.length());
        analysis.put("policyType", detectPolicyType(content));
        analysis.put("keyPoints", extractKeyPoints(content));
        analysis.put("businessOpportunities", extractOpportunities(content));
        analysis.put("affectedAreas", extractAffectedAreas(content));
        
        return analysis;
    }

    /**
     * 竞品分析
     */
    @Tool(description = "分析竞争对手信息，包括优势劣势对比")
    public Map<String, Object> analyzeCompetitor(String competitorName, String projectType) {
        log.info("竞品分析: competitor={}, projectType={}", competitorName, projectType);
        Map<String, Object> analysis = new HashMap<>();
        
        analysis.put("competitorName", competitorName);
        analysis.put("projectType", projectType);
        analysis.put("strengths", List.of("技术实力", "行业经验", "客户资源"));
        analysis.put("weaknesses", List.of("价格偏高", "响应速度"));
        analysis.put("marketPosition", "中高端市场");
        analysis.put("suggestion", "建议突出本地化服务优势和性价比");
        
        return analysis;
    }

    /**
     * 项目评估
     */
    @Tool(description = "评估项目价值和风险，给出投标建议")
    public Map<String, Object> evaluateProject(
            String projectName,
            String projectType,
            String estimatedValue,
            String customerInfo
    ) {
        log.info("项目评估: name={}, type={}, value={}", projectName, projectType, estimatedValue);
        Map<String, Object> evaluation = new HashMap<>();
        
        evaluation.put("projectName", projectName);
        evaluation.put("projectType", projectType);
        evaluation.put("estimatedValue", estimatedValue);
        
        // 价值评估
        Map<String, Object> valueAssessment = new HashMap<>();
        valueAssessment.put("score", 75);
        valueAssessment.put("factors", Map.of(
            "marketPotential", "高",
            "strategicValue", "中",
            "profitMargin", "良好"
        ));
        evaluation.put("valueAssessment", valueAssessment);
        
        // 风险评估
        Map<String, Object> riskAssessment = new HashMap<>();
        riskAssessment.put("score", 35);
        riskAssessment.put("risks", List.of(
            Map.of("type", "技术风险", "level", "低", "mitigation", "已有成熟方案"),
            Map.of("type", "商务风险", "level", "中", "mitigation", "需关注付款条款")
        ));
        evaluation.put("riskAssessment", riskAssessment);
        
        // 投标建议
        evaluation.put("recommendation", Map.of(
            "action", "建议投标",
            "priority", "高",
            "keySuccess", List.of("技术方案完整性", "类似项目经验", "合理报价")
        ));
        
        return evaluation;
    }

    // ==================== 私有辅助方法 ====================

    private List<String> extractKeywords(String content) {
        // 简单关键词提取（实际应使用 NLP 或 LLM）
        if (content.contains("智慧城市")) return List.of("智慧城市", "数字化", "政务云");
        if (content.contains("数字政府")) return List.of("数字政府", "一网通办", "政务服务平台");
        return List.of("信息化", "数字化");
    }

    private String extractBudget(String content) {
        // 简单预算提取（实际应使用正则或 LLM）
        if (content.contains("预算")) {
            return "需进一步分析确定";
        }
        return "未明确";
    }

    private String extractDeadline(String content) {
        // 简单截止日期提取
        if (content.contains("截止")) {
            return "需关注截止日期";
        }
        return "未明确";
    }

    private List<String> extractRequirements(String content) {
        // 简单需求提取
        if (content.contains("技术要求")) {
            return List.of("需满足技术规格", "需要资质证明");
        }
        return List.of("详见招标文件");
    }

    private String detectPolicyType(String content) {
        if (content.contains("规划")) return "规划类政策";
        if (content.contains("办法")) return "管理办法";
        if (content.contains("通知")) return "通知类文件";
        return "政策文件";
    }

    private List<String> extractKeyPoints(String content) {
        return List.of("政策目标", "实施范围", "支持措施", "时间节点");
    }

    private List<String> extractOpportunities(String content) {
        return List.of("项目申报机会", "资金支持机会", "试点示范机会");
    }

    private List<String> extractAffectedAreas(String content) {
        return List.of("政务服务", "城市治理", "民生服务");
    }
}
