package com.aispace.agent.agents;

import io.agentscope.core.model.ChatModelBase;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 文档助手 - 基于 AgentScope ReActAgent
 * 负责文档分析和处理
 */
@Slf4j
@Component
public class DocAgent extends AISpaceAgent {

    private static final String SYSTEM_PROMPT = """
        你是 AI Space 的文档助手，专门负责文档分析和处理。
        
        你的职责包括：
        1. 分析文档内容和结构
        2. 提取关键信息和摘要
        3. 生成文档报告
        4. 回答文档相关问题
        
        处理文档时请：
        - 准确理解文档内容
        - 提取关键数据和结论
        - 提供结构化的分析结果
        
        使用中文回复。
        """;

    public DocAgent(ChatModelBase chatModel) {
        super("doc-agent", "文档助手", "文档分析和处理助手", chatModel, SYSTEM_PROMPT);
        log.info("Doc Agent 初始化完成");
    }

    /**
     * 分析文档内容
     */
    public String analyzeDocument(String content, String docType) {
        String prompt = String.format("""
            请分析以下 %s 文档：
            
            %s
            
            请提供：
            1. 文档摘要
            2. 关键信息提取
            3. 重要结论
            """, docType, content);
        return chat(prompt);
    }

    /**
     * 生成文档摘要
     */
    public String generateSummary(String content, int maxLength) {
        String prompt = String.format("""
            请为以下内容生成摘要（不超过 %d 字）：
            
            %s
            """, maxLength, content);
        return chat(prompt);
    }

    /**
     * 提取关键信息
     */
    public String extractKeyInfo(String content, String[] keyPoints) {
        String prompt = String.format("""
            请从以下内容中提取以下关键信息：%s
            
            内容：
            %s
            
            请以结构化格式返回提取结果。
            """, String.join(", ", keyPoints), content);
        return chat(prompt);
    }

    /**
     * 回答文档相关问题
     */
    public String answerDocumentQuestion(String content, String question) {
        String prompt = String.format("""
            基于以下文档内容回答问题：
            
            文档内容：
            %s
            
            问题：%s
            """, content, question);
        return chat(prompt);
    }
}
