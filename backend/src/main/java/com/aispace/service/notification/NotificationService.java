package com.aispace.service.notification;

import com.aispace.entity.BiddingItem;
import com.aispace.entity.Reminder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * 统一通知服务
 * 管理多渠道消息推送
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {
    
    private final FeishuService feishuService;
    private final TelegramService telegramService;
    
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    
    // 默认启用的通知渠道
    private List<String> enabledChannels = new ArrayList<>(List.of("desktop"));
    
    /**
     * 获取所有通知渠道
     */
    public List<NotificationChannel> getAllChannels() {
        return List.of(feishuService, telegramService);
    }
    
    /**
     * 获取已配置的渠道
     */
    public List<NotificationChannel> getConfiguredChannels() {
        List<NotificationChannel> configured = new ArrayList<>();
        if (feishuService.isConfigured()) configured.add(feishuService);
        if (telegramService.isConfigured()) configured.add(telegramService);
        return configured;
    }
    
    /**
     * 获取渠道配置状态
     */
    public Map<String, Boolean> getChannelStatus() {
        Map<String, Boolean> status = new HashMap<>();
        status.put("feishu", feishuService.isConfigured());
        status.put("telegram", telegramService.isConfigured());
        status.put("desktop", true); // 桌面通知始终可用
        return status;
    }
    
    /**
     * 设置启用的通知渠道
     */
    public void setEnabledChannels(List<String> channels) {
        this.enabledChannels = new ArrayList<>(channels);
        log.info("Enabled notification channels: {}", channels);
    }
    
    public List<String> getEnabledChannels() {
        return new ArrayList<>(enabledChannels);
    }
    
    /**
     * 发送文本消息到所有启用的渠道
     */
    public CompletableFuture<Map<String, Boolean>> sendText(String message) {
        return sendToAllChannels(channel -> channel.sendText(message));
    }
    
    /**
     * 发送招标提醒到所有启用的渠道
     */
    public CompletableFuture<Map<String, Boolean>> sendBiddingAlert(BiddingItem item) {
        NotificationChannel.BiddingAlert alert = new NotificationChannel.BiddingAlert(
            item.getTitle(),
            item.getPurchaser() != null ? item.getPurchaser() : "未知",
            formatBudget(item.getBudget()),
            item.getDeadline() != null ? item.getDeadline().format(DATE_FORMATTER) : "未知",
            item.getMatchReason() != null ? item.getMatchReason() : "关键词匹配",
            item.getUrl()
        );
        
        return sendToAllChannels(channel -> channel.sendBiddingAlert(alert));
    }
    
    /**
     * 发送任务提醒到所有启用的渠道
     */
    public CompletableFuture<Map<String, Boolean>> sendReminderAlert(Reminder reminder) {
        NotificationChannel.ReminderAlert alert = new NotificationChannel.ReminderAlert(
            reminder.getTitle(),
            reminder.getDescription() != null ? reminder.getDescription() : "",
            reminder.getRemindAt() != null ? reminder.getRemindAt().format(DATE_FORMATTER) : "未知",
            buildRelatedEntity(reminder)
        );
        
        return sendToAllChannels(channel -> channel.sendReminderAlert(alert));
    }
    
    /**
     * 发送自定义卡片消息
     */
    public CompletableFuture<Map<String, Boolean>> sendCard(String title, String content, Map<String, String> actions) {
        return sendToAllChannels(channel -> channel.sendCard(title, content, actions));
    }
    
    /**
     * 发送消息到指定渠道
     */
    public CompletableFuture<Boolean> sendToChannel(String channelName, String message) {
        NotificationChannel channel = getChannelByName(channelName);
        if (channel != null && channel.isConfigured()) {
            return channel.sendText(message);
        }
        log.warn("Channel not available: {}", channelName);
        return CompletableFuture.completedFuture(false);
    }
    
    /**
     * 批量发送招标提醒
     */
    public CompletableFuture<Integer> sendBiddingAlerts(List<BiddingItem> items) {
        return CompletableFuture.supplyAsync(() -> {
            int successCount = 0;
            for (BiddingItem item : items) {
                try {
                    Map<String, Boolean> results = sendBiddingAlert(item).join();
                    if (results.values().stream().anyMatch(v -> v)) {
                        successCount++;
                    }
                } catch (Exception e) {
                    log.error("Failed to send alert for item: {}", item.getId(), e);
                }
            }
            log.info("Sent {} bidding alerts out of {}", successCount, items.size());
            return successCount;
        });
    }
    
    private CompletableFuture<Map<String, Boolean>> sendToAllChannels(
            java.util.function.Function<NotificationChannel, CompletableFuture<Boolean>> sender) {
        
        Map<String, CompletableFuture<Boolean>> futures = new HashMap<>();
        
        for (NotificationChannel channel : getAllChannels()) {
            if (enabledChannels.contains(channel.getChannelName()) && channel.isConfigured()) {
                futures.put(channel.getChannelName(), sender.apply(channel));
            }
        }
        
        return CompletableFuture.supplyAsync(() -> {
            Map<String, Boolean> results = new HashMap<>();
            for (Map.Entry<String, CompletableFuture<Boolean>> entry : futures.entrySet()) {
                try {
                    results.put(entry.getKey(), entry.getValue().join());
                } catch (Exception e) {
                    log.error("Failed to send to channel: {}", entry.getKey(), e);
                    results.put(entry.getKey(), false);
                }
            }
            return results;
        });
    }
    
    private NotificationChannel getChannelByName(String name) {
        return switch (name) {
            case "feishu" -> feishuService;
            case "telegram" -> telegramService;
            default -> null;
        };
    }
    
    private String formatBudget(BigDecimal budget) {
        if (budget == null) return "未知";
        if (budget.compareTo(new BigDecimal("10000")) >= 0) {
            return budget.divide(new BigDecimal("10000")).setScale(2, java.math.RoundingMode.HALF_UP) + "万元";
        }
        return budget + "元";
    }
    
    private String buildRelatedEntity(Reminder reminder) {
        if (reminder.getProjectId() != null) {
            return "项目: " + reminder.getProjectId();
        }
        if (reminder.getCustomerId() != null) {
            return "客户: " + reminder.getCustomerId();
        }
        return "无关联";
    }
    
    /**
     * 测试通知渠道
     */
    public CompletableFuture<Map<String, Boolean>> testAllChannels() {
        return sendText("这是一条测试消息 - AI员工协作系统");
    }
}
