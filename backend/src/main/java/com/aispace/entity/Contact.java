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

import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

/**
 * 联系人实体
 * 敏感字段（name, phone, mobile, email, wechat, notes, preferences）已加密存储
 */
@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Table(name = "contacts")
@EntityListeners(EncryptionEntityListener.class)
public class Contact extends BaseEntity {
    
    @Column(name = "customer_id", nullable = false)
    private UUID customerId;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", insertable = false, updatable = false)
    private Customer customer;
    
    @Encrypted
    @Column(nullable = false, length = 100)
    private String name;
    
    @Column(length = 100)
    private String title;
    
    @Column(length = 100)
    private String department;
    
    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private ContactRole role;
    
    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    @Builder.Default
    private ContactImportance importance = ContactImportance.NORMAL;
    
    @Encrypted
    @Column(length = 50)
    private String phone;
    
    @Encrypted
    @Column(length = 50)
    private String mobile;
    
    @Encrypted
    @Column(length = 200)
    private String email;
    
    @Encrypted
    @Column(length = 100)
    private String wechat;
    
    private LocalDate birthday;
    
    @Encrypted
    @Type(JsonType.class)
    @Column(columnDefinition = "jsonb")
    private Map<String, String> preferences;
    
    @Encrypted
    @Column(columnDefinition = "TEXT")
    private String notes;
    
    @Column(name = "is_active")
    @Builder.Default
    private Boolean isActive = true;
    
    /**
     * 联系人角色枚举
     */
    public enum ContactRole {
        DECISION_MAKER, // 决策者
        INFLUENCER,     // 影响者
        USER,           // 使用者
        CHAMPION        // 支持者
    }
    
    /**
     * 重要性枚举
     */
    public enum ContactImportance {
        KEY,            // 关键
        NORMAL,         // 普通
        LOW             // 低
    }
}
