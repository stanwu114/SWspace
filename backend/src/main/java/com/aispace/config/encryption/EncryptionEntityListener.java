package com.aispace.config.encryption;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import jakarta.persistence.*;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * JPA 实体加密监听器
 * 在实体持久化和加载时自动处理加密字段
 * 
 * 工作原理：
 * 1. @PrePersist / @PreUpdate - 在保存前加密标记字段
 * 2. @PostLoad - 在加载后解密标记字段
 * 3. 使用反射操作字段，对业务代码透明
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EncryptionEntityListener {

    private final EncryptionService encryptionService;
    
    // 缓存实体类的加密字段，避免重复反射扫描
    private final ConcurrentHashMap<Class<?>, List<Field>> encryptedFieldsCache = new ConcurrentHashMap<>();
    
    /**
     * 保存前加密
     */
    @PrePersist
    @PreUpdate
    public void prePersist(Object entity) {
        encryptEntity(entity);
    }
    
    /**
     * 加载后解密
     */
    @PostLoad
    public void postLoad(Object entity) {
        decryptEntity(entity);
    }
    
    /**
     * 加密实体中的标记字段
     */
    public void encryptEntity(Object entity) {
        if (entity == null) {
            return;
        }
        
        List<Field> encryptedFields = getEncryptedFields(entity.getClass());
        
        for (Field field : encryptedFields) {
            try {
                field.setAccessible(true);
                Object value = field.get(entity);
                
                if (value instanceof String strValue) {
                    // 检查是否已加密，避免重复加密
                    if (!encryptionService.isEncrypted(strValue) && !strValue.isEmpty()) {
                        String encrypted = encryptionService.encrypt(strValue);
                        field.set(entity, encrypted);
                        log.debug("字段 {}.{} 已加密", entity.getClass().getSimpleName(), field.getName());
                    }
                }
            } catch (Exception e) {
                log.error("加密字段 {}.{} 失败", entity.getClass().getSimpleName(), field.getName(), e);
                throw new EncryptionService.EncryptionException("加密失败", e);
            }
        }
    }
    
    /**
     * 解密实体中的标记字段
     */
    public void decryptEntity(Object entity) {
        if (entity == null) {
            return;
        }
        
        List<Field> encryptedFields = getEncryptedFields(entity.getClass());
        
        for (Field field : encryptedFields) {
            try {
                field.setAccessible(true);
                Object value = field.get(entity);
                
                if (value instanceof String strValue) {
                    // 检查是否已加密
                    if (encryptionService.isEncrypted(strValue)) {
                        String decrypted = encryptionService.decrypt(strValue);
                        field.set(entity, decrypted);
                        log.debug("字段 {}.{} 已解密", entity.getClass().getSimpleName(), field.getName());
                    }
                }
            } catch (Exception e) {
                log.error("解密字段 {}.{} 失败", entity.getClass().getSimpleName(), field.getName(), e);
                // 解密失败不抛出异常，保留原始值
            }
        }
    }
    
    /**
     * 获取实体类的加密字段列表（带缓存）
     */
    private List<Field> getEncryptedFields(Class<?> clazz) {
        return encryptedFieldsCache.computeIfAbsent(clazz, k -> {
            // 获取所有字段（包括继承的）
            List<Field> allFields = Arrays.asList(clazz.getDeclaredFields());
            
            // 过滤出有 @Encrypted 注解的字段
            return allFields.stream()
                .filter(field -> field.isAnnotationPresent(Encrypted.class))
                .filter(field -> {
                    Encrypted annotation = field.getAnnotation(Encrypted.class);
                    return annotation.enabled() && field.getType() == String.class;
                })
                .toList();
        });
    }
}
