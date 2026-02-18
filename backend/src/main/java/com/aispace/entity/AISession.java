package com.aispace.entity;

import io.hypersistence.utils.hibernate.type.json.JsonType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Type;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * AI会话实体
 */
@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Table(name = "ai_sessions")
public class AISession extends BaseEntity {
    
    @Enumerated(EnumType.STRING)
    @Column(name = "agent_type", nullable = false, length = 20)
    private AgentType agentType;
    
    @Column(length = 500)
    private String title;
    
    @Column(name = "project_id")
    private UUID projectId;
    
    @Column(name = "customer_id")
    private UUID customerId;
    
    @Column(name = "document_id")
    private UUID documentId;
    
    @Type(JsonType.class)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> context;
    
    @Column(columnDefinition = "TEXT")
    private String summary;
    
    @Column(name = "message_count")
    @Builder.Default
    private Integer messageCount = 0;
    
    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    @Builder.Default
    private SessionStatus status = SessionStatus.ACTIVE;
    
    @Column(name = "last_message_at")
    private LocalDateTime lastMessageAt;
    
    /**
     * Agent类型枚举
     */
    public enum AgentType {
        INTEL,          // 情报分析师
        DOC,            // 文档写手
        CRM,            // 客户助理
        KNOWLEDGE       // 知识管家
    }
    
    /**
     * 会话状态枚举
     */
    public enum SessionStatus {
        ACTIVE,         // 活跃
        ARCHIVED,       // 已归档
        DELETED         // 已删除
    }
}
