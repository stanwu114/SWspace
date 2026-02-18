package com.aispace.entity;

import io.hypersistence.utils.hibernate.type.json.JsonType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Type;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * AI消息实体
 */
@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
@Table(name = "ai_messages")
public class AIMessage {
    
    @Id
    @GeneratedValue(generator = "uuid2")
    @GenericGenerator(name = "uuid2", strategy = "uuid2")
    @Column(columnDefinition = "uuid")
    private UUID id;
    
    @Column(name = "session_id", nullable = false)
    private UUID sessionId;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", insertable = false, updatable = false)
    private AISession session;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MessageRole role;
    
    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;
    
    @Column(name = "tool_name", length = 100)
    private String toolName;
    
    @Type(JsonType.class)
    @Column(name = "tool_input", columnDefinition = "jsonb")
    private Map<String, Object> toolInput;
    
    @Type(JsonType.class)
    @Column(name = "tool_output", columnDefinition = "jsonb")
    private Map<String, Object> toolOutput;
    
    @Column(length = 50)
    private String model;
    
    @Column(name = "tokens_input")
    private Integer tokensInput;
    
    @Column(name = "tokens_output")
    private Integer tokensOutput;
    
    @Column(name = "latency_ms")
    private Integer latencyMs;
    
    @Enumerated(EnumType.STRING)
    @Column(length = 10)
    private MessageFeedback feedback;
    
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    /**
     * 消息角色枚举
     */
    public enum MessageRole {
        USER,           // 用户
        ASSISTANT,      // AI助手
        SYSTEM,         // 系统
        TOOL            // 工具调用
    }
    
    /**
     * 消息反馈枚举
     */
    public enum MessageFeedback {
        GOOD,           // 好评
        BAD,            // 差评
        NONE            // 无反馈
    }
}
