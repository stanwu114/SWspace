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
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 案例实体
 * 管理项目案例，用于知识沉淀和复用
 */
@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Table(name = "cases", indexes = {
    @Index(name = "idx_case_status", columnList = "status"),
    @Index(name = "idx_case_industry", columnList = "industry"),
    @Index(name = "idx_case_region", columnList = "region")
})
public class Case extends BaseEntity {
    
    @Column(nullable = false, length = 300)
    private String title;
    
    @Column(columnDefinition = "TEXT")
    private String description;
    
    @Column(name = "customer_name", length = 200)
    private String customerName;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "customer_type", length = 30)
    private CustomerType customerType;
    
    @Column(length = 100)
    private String industry;
    
    @Column(length = 100)
    private String region;
    
    @Column(name = "project_id")
    private UUID projectId;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", insertable = false, updatable = false)
    private Project project;
    
    @Column(name = "contract_value", precision = 15, scale = 2)
    private BigDecimal contractValue;
    
    @Column(name = "start_date")
    private LocalDate startDate;
    
    @Column(name = "end_date")
    private LocalDate endDate;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private CaseStatus status = CaseStatus.DRAFT;
    
    @Column(columnDefinition = "TEXT")
    private String background;
    
    @Column(columnDefinition = "TEXT")
    private String challenge;
    
    @Column(columnDefinition = "TEXT")
    private String solution;
    
    @Column(columnDefinition = "TEXT")
    private String outcome;
    
    @Type(JsonType.class)
    @Column(name = "key_highlights", columnDefinition = "jsonb")
    private List<String> keyHighlights;
    
    @Type(JsonType.class)
    @Column(name = "tech_stack", columnDefinition = "jsonb")
    private List<String> techStack;
    
    @Type(JsonType.class)
    @Column(columnDefinition = "jsonb")
    @Builder.Default
    private List<String> tags = List.of();
    
    @Type(JsonType.class)
    @Column(name = "metrics", columnDefinition = "jsonb")
    private Map<String, Object> metrics;
    
    @Type(JsonType.class)
    @Column(name = "lessons_learned", columnDefinition = "jsonb")
    private List<String> lessonsLearned;
    
    @Type(JsonType.class)
    @Column(name = "attachments", columnDefinition = "jsonb")
    private List<Map<String, String>> attachments;
    
    @Column(name = "is_public")
    @Builder.Default
    private Boolean isPublic = false;
    
    @Column(name = "is_featured")
    @Builder.Default
    private Boolean isFeatured = false;
    
    @Column(name = "view_count")
    @Builder.Default
    private Integer viewCount = 0;
    
    @Column(name = "reference_count")
    @Builder.Default
    private Integer referenceCount = 0;
    
    @Column(length = 100)
    private String author;
    
    /**
     * 案例状态枚举
     */
    public enum CaseStatus {
        DRAFT,          // 草稿
        UNDER_REVIEW,   // 审核中
        PUBLISHED,      // 已发布
        ARCHIVED        // 已归档
    }
    
    /**
     * 客户类型枚举
     */
    public enum CustomerType {
        GOVERNMENT,     // 政府
        ENTERPRISE,     // 企业
        EDUCATION,      // 教育
        HEALTHCARE,     // 医疗
        FINANCE,        // 金融
        OTHER           // 其他
    }
}
