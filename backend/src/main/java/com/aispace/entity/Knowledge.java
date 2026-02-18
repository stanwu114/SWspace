package com.aispace.entity;

import io.hypersistence.utils.hibernate.type.json.JsonType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Type;

import java.time.LocalDate;
import java.util.List;

/**
 * 知识库实体
 */
@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Table(name = "knowledge")
public class Knowledge extends BaseEntity {
    
    @Column(nullable = false, length = 500)
    private String title;
    
    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;
    
    @Column(columnDefinition = "TEXT")
    private String summary;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private KnowledgeCategory category;
    
    @Column(length = 50)
    private String subcategory;
    
    @Type(JsonType.class)
    @Column(columnDefinition = "jsonb")
    @Builder.Default
    private List<String> tags = List.of();
    
    @Type(JsonType.class)
    @Column(columnDefinition = "jsonb")
    @Builder.Default
    private List<String> keywords = List.of();
    
    @Enumerated(EnumType.STRING)
    @Column(name = "source_type", length = 20)
    private KnowledgeSourceType sourceType;
    
    @Column(name = "source_id")
    private java.util.UUID sourceId;
    
    @Column(name = "source_name", length = 500)
    private String sourceName;
    
    @Column(length = 100)
    private String author;
    
    @Column(name = "effective_date")
    private LocalDate effectiveDate;
    
    @Column(name = "expire_date")
    private LocalDate expireDate;
    
    // 向量字段由数据库直接处理，这里不映射
    // @Column(name = "embedding")
    // private float[] embedding;
    
    @Column(name = "view_count")
    @Builder.Default
    private Integer viewCount = 0;
    
    @Column(name = "use_count")
    @Builder.Default
    private Integer useCount = 0;
    
    @Column(precision = 3, scale = 2)
    private java.math.BigDecimal rating;
    
    @Column(name = "is_verified")
    @Builder.Default
    private Boolean isVerified = false;
    
    @Column(name = "is_featured")
    @Builder.Default
    private Boolean isFeatured = false;
    
    /**
     * 知识分类枚举
     */
    public enum KnowledgeCategory {
        INDUSTRY,       // 行业知识
        SOLUTION,       // 方案知识
        CASE,           // 案例
        SALES,          // 销售技巧
        LESSON,         // 经验教训
        TEMPLATE,       // 模板
        OTHER           // 其他
    }
    
    /**
     * 知识来源类型枚举
     */
    public enum KnowledgeSourceType {
        CASE,           // 来自案例
        EXPERIENCE,     // 来自经验
        DOCUMENT,       // 来自文档
        MANUAL,         // 手动录入
        IMPORT          // 导入
    }
}
