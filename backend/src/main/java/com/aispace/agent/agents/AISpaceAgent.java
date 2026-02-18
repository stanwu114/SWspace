package com.aispace.agent.agents;

import io.agentscope.core.ReActAgent;
import io.agentscope.core.model.ChatModelBase;
import io.agentscope.core.memory.InMemoryMemory;
import io.agentscope.core.memory.Memory;
import io.agentscope.core.message.Msg;
import io.agentscope.core.tool.Toolkit;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * AI Space 基础 Agent 类 - 基于 AgentScope ReActAgent
 * 所有业务 Agent 的基类
 */
@Slf4j
@Getter
public class AISpaceAgent {

    protected final String agentId;
    protected final String name;
    protected final String description;
    protected final ReActAgent reactAgent;
    protected final Toolkit toolkit;
    protected final Memory memory;
    protected final List<Consumer<String>> messageListeners = new ArrayList<>();

    /** 上下文窗口最大消息数（默认 50 轮，即 100 条消息） */
    private static final int DEFAULT_MAX_CONTEXT_MESSAGES = 100;
    private final int maxContextMessages;

    public AISpaceAgent(String agentId, String name, String description, 
                        ChatModelBase chatModel, String systemPrompt) {
        this(agentId, name, description, chatModel, systemPrompt, DEFAULT_MAX_CONTEXT_MESSAGES);
    }

    public AISpaceAgent(String agentId, String name, String description, 
                        ChatModelBase chatModel, String systemPrompt, int maxContextMessages) {
        this.agentId = agentId;
        this.name = name;
        this.description = description;
        this.maxContextMessages = maxContextMessages;
        this.toolkit = new Toolkit();
        this.memory = new InMemoryMemory();
        
        // 使用官方 ReActAgent Builder 构建，传入 toolkit 和 memory
        this.reactAgent = ReActAgent.builder()
                .name(name)
                .sysPrompt(systemPrompt)
                .model(chatModel)
                .toolkit(this.toolkit)
                .memory(this.memory)
                .maxIters(5)
                .build();
        
        log.info("Agent [{}] 初始化完成", name);
    }

    /**
     * 添加消息监听器
     */
    public void addMessageListener(Consumer<String> listener) {
        messageListeners.add(listener);
    }

    /**
     * 移除消息监听器
     */
    public void removeMessageListener(Consumer<String> listener) {
        messageListeners.remove(listener);
    }

    /**
     * 发送消息给 Agent（同步）
     */
    public String chat(String message) {
        try {
            log.debug("Agent [{}] 接收消息: {}", name, message);
            
            // 上下文窗口管理：在调用前检查并裁剪过长的历史
            trimContextIfNeeded();
            
            Msg userMsg = Msg.builder()
                    .textContent(message)
                    .build();
            Msg response = reactAgent.call(List.of(userMsg)).block();
            
            String responseText = response != null ? response.getTextContent() : "无响应";
            notifyListeners(responseText);
            
            return responseText;
        } catch (Exception e) {
            log.error("Agent [{}] 处理消息失败", name, e);
            return "处理失败: " + e.getMessage();
        }
    }

    /**
     * 发送消息给 Agent（流式）
     */
    public Flux<String> chatStream(String message) {
        Sinks.Many<String> sink = Sinks.many().unicast().onBackpressureBuffer();
        
        try {
            log.debug("Agent [{}] 接收流式消息: {}", name, message);
            
            // 上下文窗口管理
            trimContextIfNeeded();
            
            Msg userMsg = Msg.builder()
                    .textContent(message)
                    .build();
            reactAgent.call(List.of(userMsg))
                    .doOnSuccess(msg -> {
                        if (msg != null) {
                            String content = msg.getTextContent();
                            if (content != null && !content.isEmpty()) {
                                sink.tryEmitNext(content);
                            }
                        }
                        sink.tryEmitComplete();
                    })
                    .doOnError(error -> {
                        log.error("Agent [{}] 流式处理失败", name, error);
                        sink.tryEmitError(error);
                    })
                    .subscribe();
                    
        } catch (Exception e) {
            log.error("Agent [{}] 启动流式处理失败", name, e);
            sink.tryEmitError(e);
        }
        
        return sink.asFlux();
    }

    /**
     * 裁剪上下文窗口 - 当消息数超过阈值时删除最早的消息
     */
    protected void trimContextIfNeeded() {
        try {
            List<Msg> messages = memory.getMessages();
            int excess = messages.size() - maxContextMessages;
            if (excess > 0) {
                log.debug("Agent [{}] 上下文窗口裁剪: 当前 {} 条，超出 {} 条", 
                    name, messages.size(), excess);
                // 从头部删除最旧的消息
                for (int i = 0; i < excess; i++) {
                    memory.deleteMessage(0);
                }
            }
        } catch (Exception e) {
            log.warn("Agent [{}] 上下文裁剪失败", name, e);
        }
    }

    /**
     * 清除所有上下文
     */
    public void clearContext() {
        memory.clear();
        log.info("Agent [{}] 上下文已清除", name);
    }

    /**
     * 获取当前上下文消息数
     */
    public int getContextSize() {
        return memory.getMessages().size();
    }

    /**
     * 获取 Agent 状态
     */
    public AgentStatus getStatus() {
        return AgentStatus.builder()
                .agentId(agentId)
                .name(name)
                .description(description)
                .status("运行中")
                .contextSize(getContextSize())
                .maxContextSize(maxContextMessages)
                .build();
    }

    /**
     * 通知所有监听器
     */
    protected void notifyListeners(String message) {
        for (Consumer<String> listener : messageListeners) {
            try {
                listener.accept(message);
            } catch (Exception e) {
                log.error("通知监听器失败", e);
            }
        }
    }

    /**
     * Agent 状态信息
     */
    @lombok.Builder
    @lombok.Data
    public static class AgentStatus {
        private String agentId;
        private String name;
        private String description;
        private String status;
        private int contextSize;
        private int maxContextSize;
    }
}
