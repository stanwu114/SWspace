package com.aispace.controller;

import com.aispace.agent.service.AgentManagerService;
import com.aispace.entity.AIMessage;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Agent 控制器 - 基于 AgentScope 官方框架
 * 提供 AI Agent 对话和状态查询接口
 */
@Slf4j
@RestController
@RequestMapping("/api/agents")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class AgentController {

    private final AgentManagerService agentManagerService;

    /**
     * 发送消息给 Agent（同步）
     */
    @PostMapping("/{agentType}/chat")
    public ResponseEntity<ChatResponse> chat(
            @PathVariable String agentType,
            @RequestBody ChatRequest request) {
        
        String sessionId = request.getSessionId() != null 
            ? request.getSessionId() 
            : UUID.randomUUID().toString();
        
        log.info("Agent [{}] 收到消息, session: {}", agentType, sessionId);
        
        try {
            String response = agentManagerService.chat(
                agentType, 
                sessionId, 
                request.getMessage(),
                request.getUserId()
            );
            
            return ResponseEntity.ok(new ChatResponse(
                sessionId,
                agentType,
                response,
                "success"
            ));
        } catch (Exception e) {
            log.error("Agent 对话失败", e);
            return ResponseEntity.ok(new ChatResponse(
                sessionId,
                agentType,
                "处理失败: " + e.getMessage(),
                "error"
            ));
        }
    }

    /**
     * 发送消息给 Agent（流式）
     */
    @PostMapping(value = "/{agentType}/stream", produces = "text/event-stream")
    public Flux<String> chatStream(
            @PathVariable String agentType,
            @RequestBody ChatRequest request) {
        
        String sessionId = request.getSessionId() != null 
            ? request.getSessionId() 
            : UUID.randomUUID().toString();
        
        log.info("Agent [{}] 收到流式消息, session: {}", agentType, sessionId);
        
        return agentManagerService.chatStream(
            agentType,
            sessionId,
            request.getMessage(),
            request.getUserId()
        ).map(chunk -> "data: " + chunk + "\n\n")
         .onErrorResume(e -> {
             log.error("流式处理失败", e);
             return Flux.just("data: [ERROR] " + e.getMessage() + "\n\n");
         });
    }

    /**
     * 获取所有 Agent 状态
     */
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getAllAgentStatus() {
        return ResponseEntity.ok(agentManagerService.getAllAgentStatus());
    }

    /**
     * 获取指定 Agent 状态
     */
    @GetMapping("/{agentType}/status")
    public ResponseEntity<?> getAgentStatus(@PathVariable String agentType) {
        try {
            return ResponseEntity.ok(agentManagerService.getAgentStatus(agentType));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "error", "Agent 不存在: " + agentType
            ));
        }
    }

    /**
     * 获取会话历史
     */
    @GetMapping("/sessions/{sessionId}/history")
    public ResponseEntity<List<AIMessage>> getSessionHistory(
            @PathVariable String sessionId) {
        return ResponseEntity.ok(agentManagerService.getSessionHistory(sessionId));
    }

    /**
     * 清除会话
     */
    @DeleteMapping("/sessions/{sessionId}")
    public ResponseEntity<Map<String, String>> clearSession(
            @PathVariable String sessionId) {
        agentManagerService.clearSession(sessionId);
        return ResponseEntity.ok(Map.of(
            "message", "会话已清除",
            "sessionId", sessionId
        ));
    }

    // 请求/响应 DTO
    @Data
    public static class ChatRequest {
        private String sessionId;
        private String message;
        private Long userId;
    }

    @Data
    @RequiredArgsConstructor
    public static class ChatResponse {
        private final String sessionId;
        private final String agentType;
        private final String content;
        private final String status;
    }
}
