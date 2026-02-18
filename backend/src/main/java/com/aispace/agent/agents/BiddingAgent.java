package com.aispace.agent.agents;

import com.aispace.agent.tools.BiddingTools;
import io.agentscope.core.model.ChatModelBase;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 招投标助手 - 基于 AgentScope ReActAgent
 * 负责招投标信息监控和分析
 */
@Slf4j
@Component
public class BiddingAgent extends AISpaceAgent {

    private static final String SYSTEM_PROMPT = """
        你是 AI Space 的招投标助手，专门负责招投标信息监控和分析。
        
        你的职责包括：
        1. 监控和分析招投标信息
        2. 评估招标项目的匹配度和可行性
        3. 跟踪投标进度和状态
        4. 提供投标策略建议
        
        你可以使用以下工具：
        - list_bidding_items: 查询招标信息列表
        - get_bidding_detail: 获取招标详情
        - add_bidding_item: 添加招标信息
        - update_bidding_status: 更新招标状态
        - analyze_bidding: 分析招标信息
        
        分析招标时请重点关注：
        - 项目预算与公司能力匹配度
        - 技术要求的可行性
        - 竞争对手分析
        - 风险评估
        
        使用中文回复。
        """;

    public BiddingAgent(ChatModelBase chatModel, BiddingTools biddingTools) {
        super("bidding-agent", "招投标助手", "招投标信息监控和分析助手", chatModel, SYSTEM_PROMPT);
        
        // 注册工具
        registerTools(biddingTools);
        log.info("Bidding Agent 初始化完成，已注册 {} 个工具", 5);
    }

    private void registerTools(BiddingTools biddingTools) {
        // 使用父类的 Toolkit 实例（持久化引用，避免 GC 回收）
        toolkit.registerTool(biddingTools);
    }

    /**
     * 分析招标信息
     */
    public String analyzeBidding(Long biddingId) {
        String prompt = String.format("请详细分析招标信息 ID=%d，包括匹配度评估和风险分析。", biddingId);
        return chat(prompt);
    }

    /**
     * 获取招标建议
     */
    public String getBiddingAdvice(Long biddingId) {
        String prompt = String.format("请为招标信息 ID=%d 提供投标策略建议。", biddingId);
        return chat(prompt);
    }

    /**
     * 监控新招标
     */
    public String monitorNewBiddings() {
        String prompt = "请检查最新的招标信息，识别值得关注的项目。";
        return chat(prompt);
    }
}
