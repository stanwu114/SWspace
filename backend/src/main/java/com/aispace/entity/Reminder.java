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
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 提醒实体
 */
@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Table(name = "reminders")
public class Reminder extends BaseEntity {
    
    @Column(nullable = false, length = 500)
    private String title;
    
    @Column(columnDefinition = "TEXT")
    private String description;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ReminderType type;
    
    @Column(name = "project_id")
    private UUID projectId;
    
    @Column(name = "customer_id")
    private UUID customerId;
    
    @Column(name = "interaction_id")
    private UUID interactionId;
    
    @Column(name = "remind_at", nullable = false)
    private LocalDateTime remindAt;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "repeat_type", length = 20)
    @Builder.Default
    private RepeatType repeatType = RepeatType.NONE;
    
    @Type(JsonType.class)
    @Column(name = "repeat_config", columnDefinition = "jsonb")
    private Map<String, Object> repeatConfig;
    
    @Enumerated(EnumType.STRING)
    @Column(length = 10)
    @Builder.Default
    private ReminderPriority priority = ReminderPriority.NORMAL;
    
    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    @Builder.Default
    private ReminderStatus status = ReminderStatus.PENDING;
    
    @Column(name = "snoozed_until")
    private LocalDateTime snoozedUntil;
    
    @Type(JsonType.class)
    @Column(name = "notification_channels", columnDefinition = "jsonb")
    @Builder.Default
    private List<String> notificationChannels = List.of("desktop");
    
    /**
     * 提醒类型枚举
     */
    public enum ReminderType {
        FOLLOW_UP,      // 跟进
        DEADLINE,       // 截止日期
        PAYMENT,        // 付款
        MEETING,        // 会议
        CUSTOM          // 自定义
    }
    
    /**
     * 重复类型枚举
     */
    public enum RepeatType {
        NONE,           // 不重复
        DAILY,          // 每天
        WEEKLY,         // 每周
        MONTHLY,        // 每月
        YEARLY          // 每年
    }
    
    /**
     * 优先级枚举
     */
    public enum ReminderPriority {
        HIGH,           // 高
        NORMAL,         // 普通
        LOW             // 低
    }
    
    /**
     * 状态枚举
     */
    public enum ReminderStatus {
        PENDING,        // 待处理
        DONE,           // 已完成
        DISMISSED,      // 已忽略
        SNOOZED         // 已推迟
    }
}
