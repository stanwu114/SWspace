package com.aispace.config;

import com.aispace.config.encryption.EncryptionEntityListener;
import com.aispace.config.encryption.EncryptionService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * JPA 加密配置
 * 注册实体加密监听器
 */
@Configuration
@RequiredArgsConstructor
public class JpaEncryptionConfig {

    private final EncryptionService encryptionService;
    
    /**
     * 注册加密实体监听器
     * 注意：Spring Data JPA 的 @EntityListeners 需要在实体类上显式声明
     */
    @Bean
    public EncryptionEntityListener encryptionEntityListener() {
        return new EncryptionEntityListener(encryptionService);
    }
}
