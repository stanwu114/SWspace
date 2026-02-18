package com.aispace.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.util.Map;

/**
 * 配置管理服务
 * 负责读取和更新 application.yml 配置
 */
@Slf4j
@Service
public class ConfigService {

    private final ResourceLoader resourceLoader;
    private final ObjectMapper yamlMapper;

    @Value("spring.config.location:classpath:application.yml")
    private String configLocation;

    public ConfigService(ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
        // 创建 YAML Mapper，禁用引号转义
        YAMLFactory yamlFactory = YAMLFactory.builder()
            .disable(YAMLGenerator.Feature.WRITE_DOC_START_MARKER)
            .build();
        this.yamlMapper = new ObjectMapper(yamlFactory);
    }

    /**
     * 读取当前配置
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> readConfig() throws IOException {
        File configFile = getConfigFile();
        if (!configFile.exists()) {
            throw new IOException("配置文件不存在: " + configFile.getAbsolutePath());
        }
        return yamlMapper.readValue(configFile, Map.class);
    }

    /**
     * 更新 AI 配置
     */
    public void updateAIConfig(String endpoint, String apiKey, String model) throws IOException {
        Map<String, Object> config = readConfig();
        
        // 更新或创建 ai.openai 配置
        @SuppressWarnings("unchecked")
        Map<String, Object> ai = (Map<String, Object>) config.computeIfAbsent("ai", k -> new java.util.HashMap<>());
        @SuppressWarnings("unchecked")
        Map<String, Object> openai = (Map<String, Object>) ai.computeIfAbsent("openai", k -> new java.util.HashMap<>());
        
        openai.put("base-url", endpoint);
        openai.put("api-key", apiKey);
        openai.put("model", model);
        
        saveConfig(config);
        log.info("AI 配置已更新: endpoint={}, model={}", endpoint, model);
    }

    /**
     * 更新通知配置
     */
    public void updateNotificationConfig(String feishuWebhook, String telegramToken, String telegramChatId) throws IOException {
        Map<String, Object> config = readConfig();
        
        @SuppressWarnings("unchecked")
        Map<String, Object> notification = (Map<String, Object>) config.computeIfAbsent("notification", k -> new java.util.HashMap<>());
        
        if (feishuWebhook != null) {
            @SuppressWarnings("unchecked")
            Map<String, Object> feishu = (Map<String, Object>) notification.computeIfAbsent("feishu", k -> new java.util.HashMap<>());
            feishu.put("webhook-url", feishuWebhook);
        }
        
        if (telegramToken != null || telegramChatId != null) {
            @SuppressWarnings("unchecked")
            Map<String, Object> telegram = (Map<String, Object>) notification.computeIfAbsent("telegram", k -> new java.util.HashMap<>());
            if (telegramToken != null) telegram.put("bot-token", telegramToken);
            if (telegramChatId != null) telegram.put("chat-id", telegramChatId);
        }
        
        saveConfig(config);
        log.info("通知配置已更新");
    }

    /**
     * 保存配置到文件
     */
    private void saveConfig(Map<String, Object> config) throws IOException {
        File configFile = getConfigFile();
        yamlMapper.writerWithDefaultPrettyPrinter().writeValue(configFile, config);
    }

    /**
     * 获取配置文件
     */
    private File getConfigFile() throws IOException {
        // 首先尝试从外部路径读取
        String externalPath = System.getProperty("user.dir") + "/application.yml";
        File externalFile = new File(externalPath);
        if (externalFile.exists()) {
            return externalFile;
        }
        
        // 否则使用类路径下的配置
        Resource resource = resourceLoader.getResource("classpath:application.yml");
        return resource.getFile();
    }
}
