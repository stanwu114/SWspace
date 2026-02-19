package com.aispace.agent.agents;

import com.aispace.agent.tools.IntelTools;
import io.agentscope.core.model.ChatModelBase;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 情报分析师 Agent - 基于 AgentScope ReActAgent
 * 
 * 负责招标监控、政策解读、竞品分析、项目评估等情报相关工作。
 * 对应需求文档 3.2 节定义的情报分析师能力。
 */
@Slf4j
@Component
public class IntelAgent extends AISpaceAgent {

    private static final String SYSTEM_PROMPT = """
        你是 AI Space 的情报分析师，专门负责政企智慧城市行业的情报收集与分析工作。
        
        你的职责包括：
        1. 监控和解读招标信息，提取关键要点
        2. 分析政策文件，识别潜在商机
        3. 收集分析竞争对手信息
        4. 评估项目价值与风险，提供投标建议
        
        你可以使用以下工具：
        - listBiddingItems: 查询招标信息列表
        - getBiddingDetail: 获取招标项目详情
        - analyzeBidDocument: 分析招标文件，提取关键信息
        - analyzePolicy: 解读政策文件，分析商机
        - analyzeCompetitor: 分析竞争对手
        - evaluateProject: 评估项目价值和风险
        
        工作原则：
        - 信息准确性优先，宁可说"不确定"也不要编造
        - 分析要有数据支撑，给出理由
        - 发现风险时要明确指出
        - 提供可执行的建议
        
        回复时请保持专业、客观的态度，使用中文回复。
        """;

    public IntelAgent(ChatModelBase chatModel, IntelTools intelTools) {
        super("intel-agent", "情报分析师", "招标监控、政策解读、竞品分析、项目评估", chatModel, SYSTEM_PROMPT);
        
        // 注册情报分析工具
        toolkit.registerTool(intelTools);
        log.info("Intel Agent 初始化完成，已注册情报分析工具");
    }

    /**
     * 分析招标文件
     */
    public String analyzeBid(String content) {
        String prompt = String.format("""
            请分析以下招标文件，提取核心要点：
            
            %s
            
            请提供：
            1. 项目概述（名称、预算、时间）
            2. 技术要求摘要
            3. 评分标准分析
            4. 投标风险提示
            5. 投标建议
            """, content);
        return chat(prompt);
    }

    /**
     * 分析政策文件
     */
    public String analyzePolicyDocument(String content, String businessContext) {
        String prompt = String.format("""
            请解读以下政策文件，分析对我们业务的影响：
            
            业务背景：%s
            
            政策内容：
            %s
            
            请提供：
            1. 政策要点总结
            2. 对业务的影响分析
            3. 潜在商机识别
            4. 行动建议
            """, businessContext, content);
        return chat(prompt);
    }

    /**
     * 生成竞品分析报告
     */
    public String generateCompetitorReport(String competitorName, String projectType) {
        String prompt = String.format("""
            请生成竞品分析报告：
            
            竞争对手：%s
            项目类型：%s
            
            请提供：
            1. 竞争对手概况
            2. 优势与劣势分析
            3. 市场定位对比
            4. 竞争策略建议
            """, competitorName, projectType);
        return chat(prompt);
    }

    /**
     * 生成项目评估报告
     */
    public String generateProjectEvaluation(
            String projectName,
            String projectType,
            String estimatedValue,
            String customerInfo
    ) {
        String prompt = String.format("""
            请对以下项目进行评估：
            
            项目名称：%s
            项目类型：%s
            预估金额：%s
            客户信息：%s
            
            请提供：
            1. 项目价值评估（得分 0-100）
            2. 风险评估（得分 0-100，越低风险越小）
            3. 竞争态势分析
            4. 投标建议（建议投标/谨慎投标/不建议投标）
            5. 关键成功因素
            """, projectName, projectType, estimatedValue, customerInfo);
        return chat(prompt);
    }
}
