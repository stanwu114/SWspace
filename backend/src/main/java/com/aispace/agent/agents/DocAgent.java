package com.aispace.agent.agents;

import com.aispace.agent.tools.DocumentTools;
import io.agentscope.core.model.ChatModelBase;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 文档助手 - 基于 AgentScope ReActAgent
 * 负责文档分析和处理
 * 
 * 对应需求文档 3.3 节定义的文档写手能力。
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
        5. 辅助撰写技术方案和标书
        
        你可以使用以下工具：
        - listDocuments: 查询文档列表
        - getDocumentDetail: 获取文档详情
        - analyzeDocumentContent: 分析文档内容
        - generateOutline: 生成文档大纲
        - checkCompleteness: 检查文档完整性
        - extractSummary: 提取文档摘要
        - compareDocuments: 比较文档差异
        
        处理文档时请：
        - 准确理解文档内容
        - 提取关键数据和结论
        - 提供结构化的分析结果
        - 给出专业、可操作的建议
        
        撰写文档时请：
        - 结构清晰，层次分明
        - 用词专业但不晦涩
        - 重点突出，详略得当
        - 符合政企文档规范
        
        使用中文回复。
        """;

    public DocAgent(ChatModelBase chatModel, DocumentTools documentTools) {
        super("doc-agent", "文档助手", "文档分析和处理助手", chatModel, SYSTEM_PROMPT);
        
        // 注册文档工具
        toolkit.registerTool(documentTools);
        log.info("Doc Agent 初始化完成，已注册文档处理工具");
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
