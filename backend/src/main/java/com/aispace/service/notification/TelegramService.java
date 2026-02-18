package com.aispace.service.notification;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * Telegram Bot消息推送服务
 * 通过Telegram Bot API发送消息
 */
@Slf4j
@Service
public class TelegramService implements NotificationChannel {
    
    @Value("${notification.telegram.bot-token:}")
    private String botToken;
    
    @Value("${notification.telegram.chat-id:}")
    private String chatId;
    
    private final OkHttpClient client;
    private final ObjectMapper objectMapper;
    
    private static final String TELEGRAM_API_BASE = "https://api.telegram.org/bot";
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
    
    public TelegramService() {
        this.client = new OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .build();
        this.objectMapper = new ObjectMapper();
    }
    
    @Override
    public String getChannelName() {
        return "telegram";
    }
    
    @Override
    public boolean isConfigured() {
        return botToken != null && !botToken.isEmpty() 
            && chatId != null && !chatId.isEmpty();
    }
    
    @Override
    public CompletableFuture<Boolean> sendText(String message) {
        if (!isConfigured()) {
            log.warn("Telegram bot not configured");
            return CompletableFuture.completedFuture(false);
        }
        
        return CompletableFuture.supplyAsync(() -> {
            try {
                Map<String, Object> body = new HashMap<>();
                body.put("chat_id", chatId);
                body.put("text", message);
                body.put("parse_mode", "Markdown");
                
                return doSend("sendMessage", body);
            } catch (Exception e) {
                log.error("Failed to send Telegram message", e);
                return false;
            }
        });
    }
    
    @Override
    public CompletableFuture<Boolean> sendRichText(String title, String content) {
        String message = String.format("*%s*\n\n%s", escapeMarkdown(title), content);
        return sendText(message);
    }
    
    @Override
    public CompletableFuture<Boolean> sendCard(String title, String content, Map<String, String> actions) {
        if (!isConfigured()) {
            return CompletableFuture.completedFuture(false);
        }
        
        return CompletableFuture.supplyAsync(() -> {
            try {
                Map<String, Object> body = new HashMap<>();
                body.put("chat_id", chatId);
                body.put("text", String.format("*%s*\n\n%s", escapeMarkdown(title), content));
                body.put("parse_mode", "Markdown");
                
                // 添加内联键盘按钮
                if (actions != null && !actions.isEmpty()) {
                    java.util.List<java.util.List<Map<String, String>>> keyboard = new java.util.ArrayList<>();
                    java.util.List<Map<String, String>> row = new java.util.ArrayList<>();
                    
                    for (Map.Entry<String, String> action : actions.entrySet()) {
                        row.add(Map.of(
                            "text", action.getKey(),
                            "url", action.getValue()
                        ));
                    }
                    keyboard.add(row);
                    
                    body.put("reply_markup", Map.of("inline_keyboard", keyboard));
                }
                
                return doSend("sendMessage", body);
            } catch (Exception e) {
                log.error("Failed to send Telegram card message", e);
                return false;
            }
        });
    }
    
    @Override
    public CompletableFuture<Boolean> sendBiddingAlert(BiddingAlert alert) {
        String message = String.format(
            "🔔 *招标信息提醒*\n\n" +
            "📋 *项目*：%s\n" +
            "🏢 *采购人*：%s\n" +
            "💰 *预算*：%s\n" +
            "⏰ *截止*：%s\n" +
            "🎯 *匹配*：%s",
            escapeMarkdown(alert.title()),
            escapeMarkdown(alert.purchaser()),
            escapeMarkdown(alert.budget()),
            escapeMarkdown(alert.deadline()),
            escapeMarkdown(alert.matchReason())
        );
        
        Map<String, String> actions = new HashMap<>();
        if (alert.url() != null && !alert.url().isEmpty()) {
            actions.put("查看详情", alert.url());
        }
        
        return sendCard("招标提醒", message, actions.isEmpty() ? null : actions);
    }
    
    @Override
    public CompletableFuture<Boolean> sendReminderAlert(ReminderAlert alert) {
        String message = String.format(
            "⏰ *任务提醒*\n\n" +
            "📌 *标题*：%s\n" +
            "📝 *描述*：%s\n" +
            "🕐 *时间*：%s\n" +
            "🔗 *关联*：%s",
            escapeMarkdown(alert.title()),
            escapeMarkdown(alert.description()),
            escapeMarkdown(alert.remindAt()),
            escapeMarkdown(alert.relatedEntity())
        );
        
        return sendText(message);
    }
    
    /**
     * 发送文档文件
     */
    public CompletableFuture<Boolean> sendDocument(String filePath, String caption) {
        if (!isConfigured()) {
            return CompletableFuture.completedFuture(false);
        }
        
        return CompletableFuture.supplyAsync(() -> {
            try {
                java.io.File file = new java.io.File(filePath);
                if (!file.exists()) {
                    log.error("File not found: {}", filePath);
                    return false;
                }
                
                RequestBody requestBody = new MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("chat_id", chatId)
                    .addFormDataPart("caption", caption != null ? caption : "")
                    .addFormDataPart("document", file.getName(),
                        RequestBody.create(file, MediaType.parse("application/octet-stream")))
                    .build();
                
                Request request = new Request.Builder()
                    .url(TELEGRAM_API_BASE + botToken + "/sendDocument")
                    .post(requestBody)
                    .build();
                
                try (Response response = client.newCall(request).execute()) {
                    if (response.isSuccessful()) {
                        log.debug("Telegram document sent successfully");
                        return true;
                    } else {
                        log.error("Telegram API error: {} - {}", response.code(), response.body().string());
                        return false;
                    }
                }
            } catch (Exception e) {
                log.error("Failed to send Telegram document", e);
                return false;
            }
        });
    }
    
    private boolean doSend(String method, Map<String, Object> body) {
        try {
            String jsonBody = objectMapper.writeValueAsString(body);
            
            Request request = new Request.Builder()
                .url(TELEGRAM_API_BASE + botToken + "/" + method)
                .post(RequestBody.create(jsonBody, JSON))
                .build();
            
            try (Response response = client.newCall(request).execute()) {
                if (response.isSuccessful()) {
                    log.debug("Telegram message sent successfully");
                    return true;
                } else {
                    log.error("Telegram API error: {} - {}", response.code(), response.body().string());
                    return false;
                }
            }
        } catch (Exception e) {
            log.error("Failed to send Telegram message", e);
            return false;
        }
    }
    
    /**
     * 转义Markdown特殊字符
     */
    private String escapeMarkdown(String text) {
        if (text == null) return "";
        return text
            .replace("_", "\\_")
            .replace("*", "\\*")
            .replace("[", "\\[")
            .replace("]", "\\]")
            .replace("`", "\\`");
    }
    
    /**
     * 更新Bot配置
     */
    public void updateConfig(String botToken, String chatId) {
        this.botToken = botToken;
        this.chatId = chatId;
        log.info("Telegram config updated");
    }
    
    /**
     * 验证Bot配置是否有效
     */
    public CompletableFuture<Boolean> validateConfig() {
        if (!isConfigured()) {
            return CompletableFuture.completedFuture(false);
        }
        
        return CompletableFuture.supplyAsync(() -> {
            try {
                Request request = new Request.Builder()
                    .url(TELEGRAM_API_BASE + botToken + "/getMe")
                    .get()
                    .build();
                
                try (Response response = client.newCall(request).execute()) {
                    return response.isSuccessful();
                }
            } catch (Exception e) {
                log.error("Failed to validate Telegram config", e);
                return false;
            }
        });
    }
}
