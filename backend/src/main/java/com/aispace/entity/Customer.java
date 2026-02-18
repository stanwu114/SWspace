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

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 客户实体
 * 敏感字段（name, address, notes, org_structure）已加密存储
 */
@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Table(name = "customers")
@EntityListeners(EncryptionEntityListener.class)
public class Customer extends BaseEntity {
    
    @Encrypted
    @Column(nullable = false, length = 200)
    private String name;
    
    @Encrypted
    @Column(name = "short_name", length = 100)
    private String shortName;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CustomerType type;
    
    @Column(length = 100)
    private String industry;
    
    @Column(length = 100)
    private String region;
    
    @Encrypted
    @Column(length = 500)
    private String address;
    
    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    @Builder.Default
    private CustomerLevel level = CustomerLevel.NORMAL;
    
    @Encrypted
    @Type(JsonType.class)
    @Column(name = "org_structure", columnDefinition = "jsonb")
    private Map<String, Object> orgStructure;
    
    @Column(name = "relationship_score")
    @Builder.Default
    private Integer relationshipScore = 50;
    
    @Type(JsonType.class)
    @Column(columnDefinition = "jsonb")
    @Builder.Default
    private List<String> tags = List.of();
    
    @Encrypted
    @Column(columnDefinition = "TEXT")
    private String notes;
    
    @Column(length = 500)
    private String website;
    
    @Column(name = "last_contact_at")
    private LocalDateTime lastContactAt;
    
    @Column(name = "next_follow_up_at")
    private LocalDateTime nextFollowUpAt;
    
    /**
     * 客户类型枚举
     */
    public enum CustomerType {
        GOVERNMENT,     // 政府
        ENTERPRISE,     // 企业
        OTHER           // 其他
    }
    
    /**
     * 客户级别枚举
     */
    public enum CustomerLevel {
        KEY,            // 重点客户
        NORMAL,         // 普通客户
        POTENTIAL       // 潜在客户
    }
}
