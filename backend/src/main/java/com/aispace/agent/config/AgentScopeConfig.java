package com.aispace.agent.config;

import io.agentscope.core.message.Msg;
import io.agentscope.core.message.TextBlock;
import io.agentscope.core.model.ChatModelBase;
import io.agentscope.core.model.ChatResponse;
import io.agentscope.core.model.GenerateOptions;
import io.agentscope.core.model.OpenAIChatModel;
import io.agentscope.core.model.ToolSchema;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.core.publisher.Flux;

import java.util.List;

/**
 * AgentScope Java 官方框架配置
 * 配置 LLM 模型和全局设置
 */
@Slf4j
@Configuration
public class AgentScopeConfig {

    @Value("${aispace.ai.openai.api-key:}")
    private String openaiApiKey;

    @Value("${aispace.ai.openai.base-url:https://api.openai.com/v1}")
    private String openaiBaseUrl;

    @Value("${aispace.ai.openai.model:gpt-4}")
    private String openaiModel;

    @Value("${aispace.ai.deepseek.api-key:}")
    private String deepseekApiKey;

    @Value("${aispace.ai.deepseek.base-url:https://api.deepseek.com/v1}")
    private String deepseekBaseUrl;

    @Value("${aispace.ai.deepseek.model:deepseek-chat}")
    private String deepseekModel;

    /**
     * 默认使用 OpenAI 兼容接口的模型
     */
    @Bean
    public ChatModelBase defaultChatModel() {
        String apiKey = openaiApiKey != null && !openaiApiKey.isEmpty() 
            ? openaiApiKey 
            : System.getenv("OPENAI_API_KEY");
        
        if (apiKey == null || apiKey.isEmpty()) {
            log.warn("未配置 API Key，将使用模拟模式");
            return createMockModel();
        }

        GenerateOptions options = GenerateOptions.builder()
                .temperature(0.7)
                .maxTokens(4096)
                .build();

        return OpenAIChatModel.builder()
                .apiKey(apiKey)
                .baseUrl(openaiBaseUrl)
                .modelName(openaiModel)
                .generateOptions(options)
                .build();
    }

    /**
     * DeepSeek 模型（中文优化）
     */
    @Bean
    public ChatModelBase deepseekChatModel() {
        String apiKey = deepseekApiKey != null && !deepseekApiKey.isEmpty()
            ? deepseekApiKey
            : System.getenv("DEEPSEEK_API_KEY");

        if (apiKey == null || apiKey.isEmpty()) {
            log.warn("未配置 DeepSeek API Key");
            return null;
        }

        GenerateOptions options = GenerateOptions.builder()
                .temperature(0.7)
                .maxTokens(4096)
                .build();

        return OpenAIChatModel.builder()
                .apiKey(apiKey)
                .baseUrl(deepseekBaseUrl)
                .modelName(deepseekModel)
                .generateOptions(options)
                .build();
    }

    /**
     * 模拟模型（用于测试或无 API Key 环境）
     */
    private ChatModelBase createMockModel() {
        return new ChatModelBase() {
            @Override
            protected Flux<ChatResponse> doStream(List<Msg> messages, List<ToolSchema> tools, GenerateOptions options) {
                String mockText = "【模拟响应】请配置有效的 API Key 以使用真实的 AI 服务。";
                ChatResponse response = ChatResponse.builder()
                        .id("mock-" + System.currentTimeMillis())
                        .content(List.of(TextBlock.builder().text(mockText).build()))
                        .finishReason("stop")
                        .build();
                return Flux.just(response);
            }

            @Override
            public String getModelName() {
                return "mock-model";
            }
        };
    }
}
