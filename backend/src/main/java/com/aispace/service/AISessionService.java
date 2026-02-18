package com.aispace.service;

import com.aispace.dto.response.PageResponse;
import com.aispace.entity.AIMessage;
import com.aispace.entity.AISession;
import com.aispace.repository.AIMessageRepository;
import com.aispace.repository.AISessionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * AI会话服务层
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AISessionService {
    
    private final AISessionRepository sessionRepository;
    private final AIMessageRepository messageRepository;
    
    /**
     * 创建会话
     */
    @Transactional
    public AISession createSession(
            AISession.AgentType agentType,
            UUID projectId,
            UUID customerId,
            UUID documentId,
            Map<String, Object> context
    ) {
        AISession session = AISession.builder()
            .agentType(agentType)
            .title("新对话")
            .projectId(projectId)
            .customerId(customerId)
            .documentId(documentId)
            .context(context)
            .build();
        
        log.info("Creating AI session for agent: {}", agentType);
        return sessionRepository.save(session);
    }
    
    /**
     * 获取会话列表
     */
    public PageResponse<AISession> listSessions(
            AISession.AgentType agentType,
            int page,
            int pageSize
    ) {
        Pageable pageable = PageRequest.of(page - 1, pageSize);
        Page<AISession> result;
        
        if (agentType != null) {
            result = sessionRepository.findByAgentTypeAndStatusOrderByUpdatedAtDesc(
                agentType, AISession.SessionStatus.ACTIVE, pageable
            );
        } else {
            result = sessionRepository.findByStatusOrderByUpdatedAtDesc(
                AISession.SessionStatus.ACTIVE, pageable
            );
        }
        
        return PageResponse.of(
            result.getContent(),
            result.getTotalElements(),
            page,
            pageSize
        );
    }
    
    /**
     * 获取会话详情
     */
    public Optional<AISession> getSession(UUID id) {
        return sessionRepository.findById(id);
    }
    
    /**
     * 获取会话消息
     */
    public List<AIMessage> getSessionMessages(UUID sessionId) {
        return messageRepository.findBySessionIdOrderByCreatedAtAsc(sessionId);
    }
    
    /**
     * 添加消息
     */
    @Transactional
    public AIMessage addMessage(
            UUID sessionId,
            AIMessage.MessageRole role,
            String content,
            String model,
            Integer tokensInput,
            Integer tokensOutput,
            Integer latencyMs
    ) {
        AIMessage message = AIMessage.builder()
            .sessionId(sessionId)
            .role(role)
            .content(content)
            .model(model)
            .tokensInput(tokensInput)
            .tokensOutput(tokensOutput)
            .latencyMs(latencyMs)
            .build();
        
        AIMessage saved = messageRepository.save(message);
        
        // 更新会话统计
        AISession session = sessionRepository.findById(sessionId)
            .orElseThrow(() -> new RuntimeException("Session not found: " + sessionId));
        session.setMessageCount(session.getMessageCount() + 1);
        session.setLastMessageAt(LocalDateTime.now());
        sessionRepository.save(session);
        
        return saved;
    }
    
    /**
     * 更新会话标题
     */
    @Transactional
    public void updateTitle(UUID sessionId, String title) {
        AISession session = sessionRepository.findById(sessionId)
            .orElseThrow(() -> new RuntimeException("Session not found: " + sessionId));
        session.setTitle(title);
        sessionRepository.save(session);
    }
    
    /**
     * 归档会话
     */
    @Transactional
    public void archiveSession(UUID sessionId) {
        AISession session = sessionRepository.findById(sessionId)
            .orElseThrow(() -> new RuntimeException("Session not found: " + sessionId));
        session.setStatus(AISession.SessionStatus.ARCHIVED);
        sessionRepository.save(session);
        log.info("Archived session: {}", sessionId);
    }
    
    /**
     * 删除会话
     */
    @Transactional
    public void deleteSession(UUID sessionId) {
        messageRepository.deleteBySessionId(sessionId);
        sessionRepository.deleteById(sessionId);
        log.info("Deleted session: {}", sessionId);
    }
    
    /**
     * 消息反馈
     */
    @Transactional
    public void feedbackMessage(UUID messageId, AIMessage.MessageFeedback feedback) {
        AIMessage message = messageRepository.findById(messageId)
            .orElseThrow(() -> new RuntimeException("Message not found: " + messageId));
        message.setFeedback(feedback);
        messageRepository.save(message);
    }
}
