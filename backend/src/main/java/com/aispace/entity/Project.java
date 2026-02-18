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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 项目实体
 * 敏感字段（description, requirements, risk_assessment）已加密存储
 */
@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Table(name = "projects")
@EntityListeners(EncryptionEntityListener.class)
public class Project extends BaseEntity {
    
    @Column(nullable = false, length = 200)
    private String name;
    
    @Column(length = 50)
    private String code;
    
    @Column(name = "customer_id")
    private UUID customerId;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", insertable = false, updatable = false)
    private Customer customer;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ProjectStatus status = ProjectStatus.LEAD;
    
    @Column(length = 50)
    private String stage;
    
    @Column(name = "estimated_value", precision = 15, scale = 2)
    private BigDecimal estimatedValue;
    
    @Column(name = "contract_value", precision = 15, scale = 2)
    private BigDecimal contractValue;
    
    @Column(name = "bid_deadline")
    private LocalDateTime bidDeadline;
    
    @Column(name = "contract_date")
    private LocalDate contractDate;
    
    @Encrypted
    @Column(columnDefinition = "TEXT")
    private String description;
    
    @Encrypted
    @Type(JsonType.class)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> requirements;
    
    @Type(JsonType.class)
    @Column(name = "tech_stack", columnDefinition = "jsonb")
    private List<String> techStack;
    
    @Encrypted
    @Type(JsonType.class)
    @Column(name = "risk_assessment", columnDefinition = "jsonb")
    private Map<String, Object> riskAssessment;
    
    @Type(JsonType.class)
    @Column(columnDefinition = "jsonb")
    @Builder.Default
    private List<String> tags = List.of();
    
    @Column(name = "created_by", length = 100)
    private String createdBy;
    
    /**
     * 项目状态枚举
     */
    public enum ProjectStatus {
        LEAD,           // 线索
        OPPORTUNITY,    // 机会
        EXECUTION,      // 执行
        COMPLETED,      // 完成
        CANCELLED       // 取消
    }
}
