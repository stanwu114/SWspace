package com.aispace.service.notification;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * 通知服务接口
 * 定义统一的消息推送能力
 */
public interface NotificationChannel {
    
    /**
     * 获取渠道名称
     */
    String getChannelName();
    
    /**
     * 检查渠道是否已配置
     */
    boolean isConfigured();
    
    /**
     * 发送文本消息
     */
    CompletableFuture<Boolean> sendText(String message);
    
    /**
     * 发送富文本消息
     */
    CompletableFuture<Boolean> sendRichText(String title, String content);
    
    /**
     * 发送卡片消息
     */
    CompletableFuture<Boolean> sendCard(String title, String content, Map<String, String> actions);
    
    /**
     * 发送招标提醒
     */
    default CompletableFuture<Boolean> sendBiddingAlert(BiddingAlert alert) {
        String content = String.format(
            "【招标提醒】\n" +
            "项目：%s\n" +
            "采购人：%s\n" +
            "预算：%s\n" +
            "截止时间：%s\n" +
            "匹配原因：%s\n" +
            "链接：%s",
            alert.title(),
            alert.purchaser(),
            alert.budget(),
            alert.deadline(),
            alert.matchReason(),
            alert.url()
        );
        return sendText(content);
    }
    
    /**
     * 发送任务提醒
     */
    default CompletableFuture<Boolean> sendReminderAlert(ReminderAlert alert) {
        String content = String.format(
            "【任务提醒】\n" +
            "标题：%s\n" +
            "描述：%s\n" +
            "时间：%s\n" +
            "关联：%s",
            alert.title(),
            alert.description(),
            alert.remindAt(),
            alert.relatedEntity()
        );
        return sendText(content);
    }
    
    /**
     * 招标提醒数据
     */
    record BiddingAlert(
        String title,
        String purchaser,
        String budget,
        String deadline,
        String matchReason,
        String url
    ) {}
    
    /**
     * 任务提醒数据
     */
    record ReminderAlert(
        String title,
        String description,
        String remindAt,
        String relatedEntity
    ) {}
}
