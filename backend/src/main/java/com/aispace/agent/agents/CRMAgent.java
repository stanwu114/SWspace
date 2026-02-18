package com.aispace.agent.agents;

import com.aispace.agent.tools.CustomerTools;
import com.aispace.agent.tools.ProjectTools;
import io.agentscope.core.model.ChatModelBase;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * CRM 智能助手 - 基于 AgentScope ReActAgent
 * 负责客户管理和项目跟进
 */
@Slf4j
@Component
public class CRMAgent extends AISpaceAgent {

    private static final String SYSTEM_PROMPT = """
        你是 AI Space 的 CRM 智能助手，专门负责客户关系管理和项目跟进。
        
        你的职责包括：
        1. 查询和管理客户信息
        2. 跟踪项目进度和状态
        3. 记录客户互动和沟通历史
        4. 提供客户关系维护建议
        
        你可以使用以下工具：
        - list_customers: 查询客户列表
        - get_customer_detail: 查询客户详情
        - create_customer: 创建新客户
        - update_customer_status: 更新客户状态
        - list_projects: 查询项目列表
        - get_project_detail: 查询项目详情
        - create_project: 创建新项目
        - update_project_progress: 更新项目进度
        - update_project_status: 更新项目状态
        
        回复时请保持专业、友好的态度，使用中文回复。
        """;

    public CRMAgent(ChatModelBase chatModel, CustomerTools customerTools, ProjectTools projectTools) {
        super("crm-agent", "CRM助手", "客户关系管理和项目跟进助手", chatModel, SYSTEM_PROMPT);
        
        // 注册工具到 Agent
        registerTools(customerTools, projectTools);
        log.info("CRM Agent 初始化完成，已注册 {} 个工具", 8);
    }

    private void registerTools(CustomerTools customerTools, ProjectTools projectTools) {
        // 使用父类的 Toolkit 实例（持久化引用，避免 GC 回收）
        toolkit.registerTool(customerTools);
        toolkit.registerTool(projectTools);
    }

    /**
     * 处理 CRM 相关查询
     */
    public String handleCRMQuery(String query) {
        return chat(query);
    }

    /**
     * 获取客户洞察
     */
    public String getCustomerInsight(Long customerId) {
        String prompt = String.format("请分析客户 ID=%d 的情况，并提供维护建议。", customerId);
        return chat(prompt);
    }

    /**
     * 生成项目报告
     */
    public String generateProjectReport(Long projectId) {
        String prompt = String.format("请为项目 ID=%d 生成一份进度报告。", projectId);
        return chat(prompt);
    }
}
