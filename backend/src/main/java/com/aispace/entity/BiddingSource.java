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

/**
 * 招标数据源实体
 * 管理招标信息采集的平台配置
 */
@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Table(name = "bidding_sources")
public class BiddingSource extends BaseEntity {
    
    @Column(nullable = false, length = 200)
    private String name;
    
    @Column(length = 500)
    private String url;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SourceType type;
    
    @Column(length = 100)
    private String region;
    
    @Column(name = "is_active")
    @Builder.Default
    private Boolean isActive = true;
    
    @Type(JsonType.class)
    @Column(name = "crawl_config", columnDefinition = "jsonb")
    private Map<String, Object> crawlConfig;
    
    @Column(name = "last_crawl_at")
    private LocalDateTime lastCrawlAt;
    
    @Column(name = "crawl_interval")
    @Builder.Default
    private Integer crawlInterval = 3600;
    
    @Type(JsonType.class)
    @Column(name = "filter_keywords", columnDefinition = "jsonb")
    private java.util.List<String> filterKeywords;
    
    @Column(name = "success_count")
    @Builder.Default
    private Integer successCount = 0;
    
    @Column(name = "fail_count")
    @Builder.Default
    private Integer failCount = 0;
    
    /**
     * 数据源类型枚举
     */
    public enum SourceType {
        NATIONAL,       // 全国级平台
        PROVINCIAL,     // 省级平台
        CITY,           // 市级平台
        INDUSTRY        // 行业平台
    }
}
