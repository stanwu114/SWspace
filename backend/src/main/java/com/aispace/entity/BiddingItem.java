package com.aispace.entity;

import io.hypersistence.utils.hibernate.type.json.JsonType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Type;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 招标信息条目实体
 * 存储从各平台采集的招标公告信息
 */
@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Table(name = "bidding_items", indexes = {
    @Index(name = "idx_bidding_source", columnList = "source_id"),
    @Index(name = "idx_bidding_deadline", columnList = "deadline"),
    @Index(name = "idx_bidding_matched", columnList = "is_matched"),
    @Index(name = "idx_bidding_region", columnList = "region")
})
public class BiddingItem extends BaseEntity {
    
    @Column(name = "source_id", nullable = false)
    private UUID sourceId;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_id", insertable = false, updatable = false)
    private BiddingSource source;
    
    @Column(nullable = false, length = 500)
    private String title;
    
    @Column(name = "project_name", length = 500)
    private String projectName;
    
    @Column(length = 300)
    private String purchaser;
    
    @Column(length = 300)
    private String agency;
    
    @Column(precision = 15, scale = 2)
    private BigDecimal budget;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "bid_type", length = 50)
    private BidType bidType;
    
    @Column(name = "publish_date")
    private LocalDate publishDate;
    
    private LocalDateTime deadline;
    
    @Column(length = 100)
    private String region;
    
    @Column(length = 100)
    private String industry;
    
    @Column(length = 1000)
    private String url;
    
    @Column(columnDefinition = "TEXT")
    private String content;
    
    @Column(columnDefinition = "TEXT")
    private String summary;
    
    @Type(JsonType.class)
    @Column(name = "ai_analysis", columnDefinition = "jsonb")
    private Map<String, Object> aiAnalysis;
    
    @Type(JsonType.class)
    @Column(name = "keywords", columnDefinition = "jsonb")
    private List<String> keywords;
    
    @Column(name = "is_matched")
    @Builder.Default
    private Boolean isMatched = false;
    
    @Column(name = "match_score")
    private Double matchScore;
    
    @Column(name = "match_reason", length = 500)
    private String matchReason;
    
    @Column(name = "is_read")
    @Builder.Default
    private Boolean isRead = false;
    
    @Column(name = "is_starred")
    @Builder.Default
    private Boolean isStarred = false;
    
    @Column(name = "project_id")
    private UUID projectId;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", insertable = false, updatable = false)
    private Project project;
    
    @Column(name = "notification_sent")
    @Builder.Default
    private Boolean notificationSent = false;
    
    @Column(name = "external_id", length = 200)
    private String externalId;
    
    /**
     * 招标类型枚举
     */
    public enum BidType {
        PUBLIC_TENDER,              // 公开招标
        INVITED_TENDER,             // 邀请招标
        COMPETITIVE_NEGOTIATION,    // 竞争性谈判
        COMPETITIVE_CONSULTATION,   // 竞争性磋商
        SINGLE_SOURCE,              // 单一来源
        INQUIRY,                    // 询价
        FRAMEWORK_AGREEMENT,        // 框架协议
        OTHER                       // 其他
    }
}
