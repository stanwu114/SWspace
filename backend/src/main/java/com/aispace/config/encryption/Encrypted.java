package com.aispace.config.encryption;

import java.lang.annotation.*;

/**
 * 字段加密注解
 * 用于标记需要加密存储的实体字段
 * 
 * 使用示例：
 * <pre>
 * @Entity
 * public class Customer {
 *     @Encrypted
 *     private String name;
 *     
 *     @Encrypted(algorithm = EncryptionAlgorithm.AES_256_GCM)
 *     private String phone;
 * }
 * </pre>
 */
@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Encrypted {
    
    /**
     * 加密算法
     */
    EncryptionAlgorithm algorithm() default EncryptionAlgorithm.AES_256_GCM;
    
    /**
     * 是否启用加密
     */
    boolean enabled() default true;
    
    /**
     * 加密算法枚举
     */
    enum EncryptionAlgorithm {
        AES_256_GCM,    // AES-256-GCM 认证加密
        AES_256_CBC     // AES-256-CBC 传统加密（不推荐新使用）
    }
}
