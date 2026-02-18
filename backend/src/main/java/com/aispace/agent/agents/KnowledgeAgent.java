package com.aispace.agent.agents;

import com.aispace.agent.tools.KnowledgeTools;
import com.aispace.agent.tools.RAGTools;
import io.agentscope.core.model.ChatModelBase;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 知识库助手 - 基于 AgentScope ReActAgent
 * 负责知识管理和问答
 */
@Slf4j
@Component
public class KnowledgeAgent extends AISpaceAgent {

    private static final String SYSTEM_PROMPT = """
        你是 AI Space 的知识库助手，专门负责知识管理和智能问答。
        
        你的职责包括：
        1. 回答用户关于产品、技术、流程等方面的问题
        2. 搜索和检索知识库内容
        3. 整理和归纳知识条目
        4. 协助添加新的知识内容
        
        **重要**：当用户提问时，请优先使用 build_context 工具从知识库检索相关背景知识，
        然后基于检索到的上下文回答用户问题。这样可以确保回答更准确、更有据可依。
        
        你可以使用以下工具：
        - rag_search: RAG混合检索，搜索最相关的知识内容
        - build_context: 构建RAG增强上下文，为回答问题提供背景知识
        - get_related_knowledge: 获取相关知识推荐
        - search_knowledge: 关键词搜索知识库
        - get_knowledge_detail: 获取知识详情
        - add_knowledge: 添加新知识条目
        - list_knowledge_categories: 获取知识分类
        - delete_knowledge: 删除知识条目
        
        回复时请确保信息准确，如果不确定请说明。
        使用中文回复用户。
        """;

    public KnowledgeAgent(ChatModelBase chatModel, KnowledgeTools knowledgeTools, RAGTools ragTools) {
        super("knowledge-agent", "知识助手", "知识库管理和问答助手", chatModel, SYSTEM_PROMPT);
        
        // 注册工具
        registerTools(knowledgeTools, ragTools);
        log.info("Knowledge Agent 初始化完成，已注册知识库工具和RAG工具");
    }

    private void registerTools(KnowledgeTools knowledgeTools, RAGTools ragTools) {
        // 使用父类的 Toolkit 实例（持久化引用，避免 GC 回收）
        toolkit.registerTool(knowledgeTools);
        toolkit.registerTool(ragTools);
    }

    /**
     * 回答知识相关问题
     */
    public String answerQuestion(String question) {
        return chat(question);
    }

    /**
     * 添加知识
     */
    public String addKnowledge(String title, String content, String category, String tags) {
        String prompt = String.format("""
            请添加以下知识条目：
            标题: %s
            内容: %s
            分类: %s
            标签: %s
            """, title, content, category, tags);
        return chat(prompt);
    }

    /**
     * 搜索知识
     */
    public String searchKnowledge(String query) {
        String prompt = "请搜索知识库，关键词: " + query;
        return chat(prompt);
    }
}
