package com.aispace.websocket;

import com.aispace.agent.service.AgentManagerService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;

/**
 * Agent WebSocket 处理器 - 处理实时 AI 对话
 * 基于 AgentScope 官方框架，使用 aiTaskExecutor 异步处理 AI 调用
 */
@Slf4j
@Component
public class AgentWebSocketHandler extends TextWebSocketHandler {

    private final AgentManagerService agentManagerService;
    private final ObjectMapper objectMapper;
    private final Executor aiTaskExecutor;
    
    // 存储会话信息
    private final Map<String, SessionInfo> sessions = new ConcurrentHashMap<>();

    public AgentWebSocketHandler(AgentManagerService agentManagerService,
                                  ObjectMapper objectMapper,
                                  @Qualifier("aiTaskExecutor") Executor aiTaskExecutor) {
        this.agentManagerService = agentManagerService;
        this.objectMapper = objectMapper;
        this.aiTaskExecutor = aiTaskExecutor;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String sessionId = session.getId();
        log.info("WebSocket 连接建立: {}", sessionId);
        
        sessions.put(sessionId, new SessionInfo(session));
        
        // 发送连接成功消息
        sendMessage(session, Map.of(
            "type", "connected",
            "sessionId", sessionId,
            "message", "连接成功"
        ));
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String sessionId = session.getId();
        String payload = message.getPayload();
        
        // 输入验证：限制消息体大小（最大 64KB）
        if (payload.length() > 65536) {
            sendError(session, "消息体过大，请缩短消息内容");
            return;
        }
        
        log.debug("收到消息 [{}]: {}字符", sessionId, payload.length());
        
        try {
            JsonNode jsonNode = objectMapper.readTree(payload);
            String type = jsonNode.get("type").asText();
            
            switch (type) {
                case "chat" -> handleChatMessage(session, jsonNode);
                case "stream" -> handleStreamMessage(session, jsonNode);
                case "clear" -> handleClearSession(session, jsonNode);
                case "ping" -> sendMessage(session, Map.of("type", "pong"));
                default -> sendError(session, "未知消息类型: " + type);
            }
        } catch (Exception e) {
            log.error("处理消息失败", e);
            sendError(session, "消息处理失败: " + e.getMessage());
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        String sessionId = session.getId();
        log.info("WebSocket 连接关闭: {}, 状态: {}", sessionId, status);
        sessions.remove(sessionId);
    }

    /**
     * 处理普通聊天消息 - 异步执行，不阻塞 WebSocket 线程
     */
    private void handleChatMessage(WebSocketSession session, JsonNode jsonNode) {
        String sessionId = jsonNode.get("sessionId").asText(session.getId());
        String agentType = jsonNode.get("agentType").asText("knowledge");
        String content = jsonNode.has("content") ? jsonNode.get("content").asText() : "";
        Long userId = jsonNode.has("userId") ? jsonNode.get("userId").asLong() : null;
        
        // 输入验证
        if (content.isBlank()) {
            sendError(session, "消息内容不能为空");
            return;
        }
        if (content.length() > 10000) {
            sendError(session, "消息内容过长，最多10000字符");
            return;
        }
        
        // 立即发送思考中状态
        sendMessage(session, Map.of(
            "type", "thinking",
            "sessionId", sessionId
        ));
        
        // 使用 aiTaskExecutor 异步执行 AI 调用，避免阻塞 WebSocket 线程
        aiTaskExecutor.execute(() -> {
            try {
                String response = agentManagerService.chat(agentType, sessionId, content, userId);
                
                sendMessage(session, Map.of(
                    "type", "response",
                    "sessionId", sessionId,
                    "agentType", agentType,
                    "content", response
                ));
            } catch (Exception e) {
                log.error("聊天处理失败", e);
                sendError(session, "处理失败: " + e.getMessage());
            }
        });
    }

    /**
     * 处理流式聊天消息
     */
    private void handleStreamMessage(WebSocketSession session, JsonNode jsonNode) {
        String sessionId = jsonNode.get("sessionId").asText(session.getId());
        String agentType = jsonNode.get("agentType").asText("knowledge");
        String content = jsonNode.has("content") ? jsonNode.get("content").asText() : "";
        Long userId = jsonNode.has("userId") ? jsonNode.get("userId").asLong() : null;
        
        // 输入验证
        if (content.isBlank()) {
            sendError(session, "消息内容不能为空");
            return;
        }
        if (content.length() > 10000) {
            sendError(session, "消息内容过长，最多10000字符");
            return;
        }
        
        try {
            // 发送开始标记
            sendMessage(session, Map.of(
                "type", "stream_start",
                "sessionId", sessionId
            ));
            
            // 调用流式接口
            Flux<String> stream = agentManagerService.chatStream(agentType, sessionId, content, userId);
            
            StringBuilder fullContent = new StringBuilder();
            
            stream.subscribe(
                chunk -> {
                    fullContent.append(chunk);
                    try {
                        sendMessage(session, Map.of(
                            "type", "stream_chunk",
                            "sessionId", sessionId,
                            "content", chunk
                        ));
                    } catch (Exception e) {
                        log.error("发送流式块失败", e);
                    }
                },
                error -> {
                    log.error("流式处理失败", error);
                    sendError(session, "流式处理失败: " + error.getMessage());
                },
                () -> {
                    // 发送结束标记
                    try {
                        sendMessage(session, Map.of(
                            "type", "stream_end",
                            "sessionId", sessionId,
                            "fullContent", fullContent.toString()
                        ));
                    } catch (Exception e) {
                        log.error("发送流式结束标记失败", e);
                    }
                }
            );
        } catch (Exception e) {
            log.error("流式聊天处理失败", e);
            sendError(session, "处理失败: " + e.getMessage());
        }
    }

    /**
     * 处理清除会话
     */
    private void handleClearSession(WebSocketSession session, JsonNode jsonNode) {
        String sessionId = jsonNode.get("sessionId").asText(session.getId());
        
        try {
            agentManagerService.clearSession(sessionId);
            sendMessage(session, Map.of(
                "type", "cleared",
                "sessionId", sessionId,
                "message", "会话已清除"
            ));
        } catch (Exception e) {
            log.error("清除会话失败", e);
            sendError(session, "清除失败: " + e.getMessage());
        }
    }

    /**
     * 发送消息
     */
    private void sendMessage(WebSocketSession session, Map<String, Object> message) {
        try {
            if (session.isOpen()) {
                String json = objectMapper.writeValueAsString(message);
                session.sendMessage(new TextMessage(json));
            }
        } catch (IOException e) {
            log.error("发送消息失败", e);
        }
    }

    /**
     * 发送错误消息
     */
    private void sendError(WebSocketSession session, String error) {
        sendMessage(session, Map.of(
            "type", "error",
            "error", error
        ));
    }

    /**
     * 会话信息
     */
    private static class SessionInfo {
        final WebSocketSession session;
        final long connectedAt;

        SessionInfo(WebSocketSession session) {
            this.session = session;
            this.connectedAt = System.currentTimeMillis();
        }
    }
}
