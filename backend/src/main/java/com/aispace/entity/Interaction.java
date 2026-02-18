package com.aispace.entity;

import com.aispace.config.encryption.Encrypted;
import com.aispace.config.encryption.EncryptionEntityListener;
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
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 交互记录实体
 * 敏感字段（content, summary, location）已加密存储
 */
@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners({AuditingEntityListener.class, EncryptionEntityListener.class})
@Table(name = "interactions")
public class Interaction {
    
    @Id
    @GeneratedValue(generator = "uuid2")
    @GenericGenerator(name = "uuid2", strategy = "uuid2")
    @Column(columnDefinition = "uuid")
    private UUID id;
    
    @Column(name = "customer_id", nullable = false)
    private UUID customerId;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", insertable = false, updatable = false)
    private Customer customer;
    
    @Column(name = "project_id")
    private UUID projectId;
    
    @Type(JsonType.class)
    @Column(name = "contact_ids", columnDefinition = "jsonb")
    @Builder.Default
    private List<UUID> contactIds = List.of();
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private InteractionType type;
    
    @Column(length = 500)
    private String subject;
    
    @Encrypted
    @Column(columnDefinition = "TEXT")
    private String content;
    
    @Encrypted
    @Column(columnDefinition = "TEXT")
    private String summary;
    
    @Type(JsonType.class)
    @Column(name = "key_points", columnDefinition = "jsonb")
    private List<String> keyPoints;
    
    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private Sentiment sentiment;
    
    @Type(JsonType.class)
    @Column(name = "next_actions", columnDefinition = "jsonb")
    private List<Map<String, Object>> nextActions;
    
    @Column(name = "next_action_at")
    private LocalDateTime nextActionAt;
    
    @Column(name = "interaction_at", nullable = false)
    private LocalDateTime interactionAt;
    
    @Column
    private Integer duration;
    
    @Encrypted
    @Column(length = 200)
    private String location;
    
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    /**
     * 交互类型枚举
     */
    public enum InteractionType {
        CALL,           // 电话
        MEETING,        // 会议
        EMAIL,          // 邮件
        WECHAT,         // 微信
        VISIT,          // 拜访
        OTHER           // 其他
    }
    
    /**
     * 情感倾向枚举
     */
    public enum Sentiment {
        POSITIVE,       // 积极
        NEUTRAL,        // 中立
        NEGATIVE        // 消极
    }
}
