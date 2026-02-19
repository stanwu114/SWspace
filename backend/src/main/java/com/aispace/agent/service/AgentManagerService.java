package com.aispace.agent.service;

import com.aispace.agent.agents.*;
import com.aispace.entity.AIMessage;
import com.aispace.repository.AIMessageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;
import reactor.core.scheduler.Schedulers;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * Agent 管理服务 - 管理所有 AI Agent 实例
 * 基于 AgentScope 官方框架
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AgentManagerService {

    private final IntelAgent intelAgent;  // 情报分析师
    private final CRMAgent crmAgent;
    private final KnowledgeAgent knowledgeAgent;
    private final BiddingAgent biddingAgent;
    private final DocAgent docAgent;
    private final AIMessageRepository messageRepository;

    /**
     * 获取指定 Agent
     */
    public AISpaceAgent getAgent(String agentType) {
        return switch (agentType.toLowerCase()) {
            case "intel" -> intelAgent;     // 情报分析师
            case "crm" -> crmAgent;
            case "knowledge" -> knowledgeAgent;
            case "bidding" -> biddingAgent;
            case "doc" -> docAgent;
            default -> knowledgeAgent; // 默认使用知识助手
        };
    }

    /**
     * 发送消息给 Agent（同步）
     */
    public String chat(String agentType, String sessionId, String message, Long userId) {
        AISpaceAgent agent = getAgent(agentType);
        
        // 保存用户消息
        saveMessage(sessionId, agentType, userId, "user", message);
        
        // 获取响应
        String response = agent.chat(message);
        
        // 保存 AI 响应
        saveMessage(sessionId, agentType, null, "assistant", response);
        
        return response;
    }

    /**
     * 发送消息给 Agent（流式）
     */
    public Flux<String> chatStream(String agentType, String sessionId, String message, Long userId) {
        AISpaceAgent agent = getAgent(agentType);
        
        // 保存用户消息
        saveMessage(sessionId, agentType, userId, "user", message);
        
        Sinks.Many<String> sink = Sinks.many().unicast().onBackpressureBuffer();
        StringBuilder fullResponse = new StringBuilder();
        
        // 添加监听器收集完整响应
        Consumer<String> listener = chunk -> {
            fullResponse.append(chunk);
        };
        agent.addMessageListener(listener);
        
        // 调用流式接口
        agent.chatStream(message)
                .subscribeOn(Schedulers.boundedElastic())
                .doOnNext(sink::tryEmitNext)
                .doOnComplete(() -> {
                    // 保存完整响应
                    saveMessage(sessionId, agentType, null, "assistant", fullResponse.toString());
                    agent.removeMessageListener(listener);
                    sink.tryEmitComplete();
                })
                .doOnError(error -> {
                    log.error("流式处理失败", error);
                    agent.removeMessageListener(listener);
                    sink.tryEmitError(error);
                })
                .subscribe();
        
        return sink.asFlux();
    }

    /**
     * 获取所有 Agent 状态
     */
    public Map<String, Object> getAllAgentStatus() {
        Map<String, Object> status = new HashMap<>();
        
        status.put("intel", intelAgent.getStatus());
        status.put("crm", crmAgent.getStatus());
        status.put("knowledge", knowledgeAgent.getStatus());
        status.put("bidding", biddingAgent.getStatus());
        status.put("doc", docAgent.getStatus());
        
        return status;
    }

    /**
     * 获取指定 Agent 状态
     */
    public AISpaceAgent.AgentStatus getAgentStatus(String agentType) {
        return getAgent(agentType).getStatus();
    }

    /**
     * 保存消息到数据库
     */
    private void saveMessage(String sessionId, String agentType, Long userId, 
                            String role, String content) {
        try {
            UUID sessionUUID = UUID.fromString(sessionId);
            AIMessage.MessageRole messageRole = AIMessage.MessageRole.valueOf(role.toUpperCase());
            
            AIMessage message = new AIMessage();
            message.setSessionId(sessionUUID);
            message.setRole(messageRole);
            message.setContent(content);
            message.setCreatedAt(LocalDateTime.now());
            
            messageRepository.save(message);
        } catch (Exception e) {
            log.error("保存消息失败", e);
        }
    }

    /**
     * 获取会话历史
     */
    public java.util.List<AIMessage> getSessionHistory(String sessionId) {
        UUID sessionUUID = UUID.fromString(sessionId);
        return messageRepository.findBySessionIdOrderByCreatedAtAsc(sessionUUID);
    }

    /**
     * 清除会话历史
     */
    public void clearSession(String sessionId) {
        UUID sessionUUID = UUID.fromString(sessionId);
        messageRepository.deleteBySessionId(sessionUUID);
    }
}
