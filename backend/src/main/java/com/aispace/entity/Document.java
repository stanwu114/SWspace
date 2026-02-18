package com.aispace.entity;

import com.aispace.config.encryption.Encrypted;
import com.aispace.config.encryption.EncryptionEntityListener;
import io.hypersistence.utils.hibernate.type.json.JsonType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Type;

import java.util.List;
import java.util.UUID;

/**
 * 文档实体
 * 敏感字段（content_text, ai_analysis, ai_summary）已加密存储
 */
@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Table(name = "documents")
@EntityListeners(EncryptionEntityListener.class)
public class Document extends BaseEntity {
    
    @Column(name = "project_id")
    private UUID projectId;
    
    @Column(name = "customer_id")
    private UUID customerId;
    
    @Column(nullable = false, length = 500)
    private String name;
    
    @Column(name = "original_name", length = 500)
    private String originalName;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private DocumentType type;
    
    @Column(name = "file_path", nullable = false, length = 1000)
    private String filePath;
    
    @Column(name = "file_size")
    private Long fileSize;
    
    @Column(name = "file_ext", length = 20)
    private String fileExt;
    
    @Column(name = "mime_type", length = 100)
    private String mimeType;
    
    @Encrypted
    @Column(name = "content_text", columnDefinition = "TEXT")
    private String contentText;
    
    @Encrypted
    @Type(JsonType.class)
    @Column(name = "ai_analysis", columnDefinition = "jsonb")
    private java.util.Map<String, Object> aiAnalysis;
    
    @Encrypted
    @Column(name = "ai_summary", columnDefinition = "TEXT")
    private String aiSummary;
    
    @Column
    @Builder.Default
    private Integer version = 1;
    
    @Column(name = "is_latest")
    @Builder.Default
    private Boolean isLatest = true;
    
    @Column(name = "parent_id")
    private UUID parentId;
    
    @Type(JsonType.class)
    @Column(columnDefinition = "jsonb")
    @Builder.Default
    private List<String> tags = List.of();
    
    /**
     * 文档类型枚举
     */
    public enum DocumentType {
        BID,            // 招标文件
        SOLUTION,       // 方案文档
        REPORT,         // 报告
        CONTRACT,       // 合同
        POLICY,         // 政策
        OTHER           // 其他
    }
}
