package com.aispace.service.notification;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * 飞书消息推送服务
 * 通过飞书机器人Webhook发送消息
 */
@Slf4j
@Service
public class FeishuService implements NotificationChannel {
    
    @Value("${notification.feishu.webhook-url:}")
    private String webhookUrl;
    
    @Value("${notification.feishu.secret:}")
    private String secret;
    
    private final OkHttpClient client;
    private final ObjectMapper objectMapper;
    
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
    
    public FeishuService() {
        this.client = new OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .build();
        this.objectMapper = new ObjectMapper();
    }
    
    @Override
    public String getChannelName() {
        return "feishu";
    }
    
    @Override
    public boolean isConfigured() {
        return webhookUrl != null && !webhookUrl.isEmpty();
    }
    
    @Override
    public CompletableFuture<Boolean> sendText(String message) {
        if (!isConfigured()) {
            log.warn("Feishu webhook not configured");
            return CompletableFuture.completedFuture(false);
        }
        
        return CompletableFuture.supplyAsync(() -> {
            try {
                Map<String, Object> body = new HashMap<>();
                body.put("msg_type", "text");
                body.put("content", Map.of("text", message));
                
                addSignature(body);
                
                return doSend(body);
            } catch (Exception e) {
                log.error("Failed to send Feishu text message", e);
                return false;
            }
        });
    }
    
    @Override
    public CompletableFuture<Boolean> sendRichText(String title, String content) {
        if (!isConfigured()) {
            return CompletableFuture.completedFuture(false);
        }
        
        return CompletableFuture.supplyAsync(() -> {
            try {
                Map<String, Object> body = new HashMap<>();
                body.put("msg_type", "post");
                
                Map<String, Object> post = new HashMap<>();
                Map<String, Object> zhCn = new HashMap<>();
                zhCn.put("title", title);
                zhCn.put("content", List.of(
                    List.of(Map.of("tag", "text", "text", content))
                ));
                post.put("zh_cn", zhCn);
                body.put("content", Map.of("post", post));
                
                addSignature(body);
                
                return doSend(body);
            } catch (Exception e) {
                log.error("Failed to send Feishu rich text message", e);
                return false;
            }
        });
    }
    
    @Override
    public CompletableFuture<Boolean> sendCard(String title, String content, Map<String, String> actions) {
        if (!isConfigured()) {
            return CompletableFuture.completedFuture(false);
        }
        
        return CompletableFuture.supplyAsync(() -> {
            try {
                Map<String, Object> body = new HashMap<>();
                body.put("msg_type", "interactive");
                
                // 构建卡片
                Map<String, Object> card = new HashMap<>();
                
                // 标题
                card.put("header", Map.of(
                    "title", Map.of("tag", "plain_text", "content", title),
                    "template", "blue"
                ));
                
                // 内容
                List<Map<String, Object>> elements = new java.util.ArrayList<>();
                elements.add(Map.of(
                    "tag", "div",
                    "text", Map.of("tag", "lark_md", "content", content)
                ));
                
                // 按钮
                if (actions != null && !actions.isEmpty()) {
                    List<Map<String, Object>> buttons = new java.util.ArrayList<>();
                    for (Map.Entry<String, String> action : actions.entrySet()) {
                        buttons.add(Map.of(
                            "tag", "button",
                            "text", Map.of("tag", "plain_text", "content", action.getKey()),
                            "type", "primary",
                            "url", action.getValue()
                        ));
                    }
                    elements.add(Map.of("tag", "action", "actions", buttons));
                }
                
                card.put("elements", elements);
                body.put("card", card);
                
                addSignature(body);
                
                return doSend(body);
            } catch (Exception e) {
                log.error("Failed to send Feishu card message", e);
                return false;
            }
        });
    }
    
    @Override
    public CompletableFuture<Boolean> sendBiddingAlert(BiddingAlert alert) {
        String cardContent = String.format(
            "**项目名称**：%s\n" +
            "**采购人**：%s\n" +
            "**预算金额**：%s\n" +
            "**截止时间**：%s\n" +
            "**匹配原因**：%s",
            alert.title(),
            alert.purchaser(),
            alert.budget(),
            alert.deadline(),
            alert.matchReason()
        );
        
        Map<String, String> actions = new HashMap<>();
        if (alert.url() != null && !alert.url().isEmpty()) {
            actions.put("查看详情", alert.url());
        }
        
        return sendCard("🔔 招标信息提醒", cardContent, actions);
    }
    
    private void addSignature(Map<String, Object> body) {
        if (secret != null && !secret.isEmpty()) {
            long timestamp = System.currentTimeMillis() / 1000;
            body.put("timestamp", String.valueOf(timestamp));
            
            try {
                String stringToSign = timestamp + "\n" + secret;
                javax.crypto.Mac mac = javax.crypto.Mac.getInstance("HmacSHA256");
                mac.init(new javax.crypto.spec.SecretKeySpec(stringToSign.getBytes(), "HmacSHA256"));
                byte[] signData = mac.doFinal(new byte[]{});
                String sign = java.util.Base64.getEncoder().encodeToString(signData);
                body.put("sign", sign);
            } catch (Exception e) {
                log.error("Failed to generate Feishu signature", e);
            }
        }
    }
    
    private boolean doSend(Map<String, Object> body) {
        try {
            String jsonBody = objectMapper.writeValueAsString(body);
            
            Request request = new Request.Builder()
                .url(webhookUrl)
                .post(RequestBody.create(jsonBody, JSON))
                .build();
            
            try (Response response = client.newCall(request).execute()) {
                if (response.isSuccessful()) {
                    log.debug("Feishu message sent successfully");
                    return true;
                } else {
                    log.error("Feishu API error: {} - {}", response.code(), response.body().string());
                    return false;
                }
            }
        } catch (Exception e) {
            log.error("Failed to send Feishu message", e);
            return false;
        }
    }
    
    /**
     * 更新Webhook配置
     */
    public void updateConfig(String webhookUrl, String secret) {
        this.webhookUrl = webhookUrl;
        this.secret = secret;
        log.info("Feishu config updated");
    }
}
