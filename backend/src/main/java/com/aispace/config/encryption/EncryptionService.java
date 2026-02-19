package com.aispace.config.encryption;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * 加密服务
 * 提供 AES-256-GCM 加密/解密功能
 * 
 * 安全特性：
 * 1. 使用 AES-256-GCM 认证加密，防止篡改
 * 2. 每次加密使用随机 IV (Initialization Vector)
 * 3. 密文格式: [16位IV长度(2字节)][IV][密文][16位认证标签]
 * 4. 密钥从环境变量或配置文件获取，支持外部密钥管理服务
 */
@Slf4j
@Service
public class EncryptionService {

    private static final String ALGORITHM = "AES";
    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int GCM_IV_LENGTH = 12;        // GCM 标准 IV 长度
    private static final int GCM_TAG_LENGTH = 128;      // GCM 认证标签长度（位）
    private static final int KEY_LENGTH = 32;           // AES-256 密钥长度（字节）
    
    // 默认开发密钥（仅用于开发环境）
    private static final String DEFAULT_DEV_KEY = "default-dev-key-change-in-prod";
    
    @Value("${aispace.security.encryption-key:}")
    private String encryptionKey;
    
    @Value("${aispace.security.strict-key-validation:true}")
    private boolean strictKeyValidation;
    
    @Value("${spring.profiles.active:dev}")
    private String activeProfile;
    
    private SecretKey secretKey;
    private final SecureRandom secureRandom = new SecureRandom();
    
    @PostConstruct
    public void init() {
        // 生产环境强制密钥检查
        validateEncryptionKey();
        
        try {
            // 从配置密钥派生 AES-256 密钥
            this.secretKey = deriveKey(encryptionKey);
            
            // 验证密钥
            String testData = "test";
            String encrypted = encrypt(testData);
            String decrypted = decrypt(encrypted);
            
            if (!testData.equals(decrypted)) {
                throw new IllegalStateException("加密服务初始化失败：密钥验证未通过");
            }
            
            log.info("加密服务初始化成功，使用 AES-256-GCM 算法");
            
            // 警告：生产环境必须使用强密钥
            if (DEFAULT_DEV_KEY.equals(encryptionKey)) {
                log.warn("⚠️ 警告：正在使用默认开发密钥，生产环境请务必设置环境变量 ENCRYPTION_KEY！");
            }
        } catch (Exception e) {
            throw new IllegalStateException("加密服务初始化失败", e);
        }
    }
    
    /**
     * 验证加密密钥配置
     * 生产环境必须配置自定义密钥
     */
    private void validateEncryptionKey() {
        boolean isProduction = "prod".equalsIgnoreCase(activeProfile) || 
                               "production".equalsIgnoreCase(activeProfile);
        
        // 如果密钥为空或使用默认值
        if (encryptionKey == null || encryptionKey.isBlank()) {
            if (isProduction && strictKeyValidation) {
                throw new IllegalStateException(
                    "❌ 生产环境必须配置加密密钥！请设置环境变量: ENCRYPTION_KEY 或配置: aispace.security.encryption-key"
                );
            }
            // 开发环境使用默认密钥
            encryptionKey = DEFAULT_DEV_KEY;
            log.warn("⚠️ 未配置加密密钥，使用默认开发密钥（不适用于生产环境）");
        }
        
        // 生产环境检查是否使用默认密钥
        if (isProduction && DEFAULT_DEV_KEY.equals(encryptionKey) && strictKeyValidation) {
            throw new IllegalStateException(
                "❌ 生产环境禁止使用默认开发密钥！请设置环境变量: ENCRYPTION_KEY"
            );
        }
        
        // 检查密钥强度
        if (encryptionKey.length() < 16) {
            log.warn("⚠️ 加密密钥长度不足（建议至少 16 字符），当前: {} 字符", encryptionKey.length());
        }
    }
    
    /**
     * 从字符串密钥派生 AES-256 密钥
     * 使用 SHA-256 哈希确保密钥长度正确
     */
    private SecretKey deriveKey(String key) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] keyBytes = digest.digest(key.getBytes(StandardCharsets.UTF_8));
        return new SecretKeySpec(keyBytes, ALGORITHM);
    }
    
    /**
     * 加密字符串
     * 
     * @param plaintext 明文
     * @return Base64编码的密文
     */
    public String encrypt(String plaintext) {
        if (plaintext == null) {
            return null;
        }
        
        try {
            // 生成随机 IV
            byte[] iv = new byte[GCM_IV_LENGTH];
            secureRandom.nextBytes(iv);
            
            // 创建 GCM 参数规范
            GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            
            // 初始化加密器
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, gcmSpec);
            
            // 执行加密
            byte[] ciphertext = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
            
            // 组合 IV 和密文: [IV长度(2字节)][IV][密文]
            ByteBuffer buffer = ByteBuffer.allocate(2 + iv.length + ciphertext.length);
            buffer.putShort((short) iv.length);
            buffer.put(iv);
            buffer.put(ciphertext);
            
            return Base64.getEncoder().encodeToString(buffer.array());
        } catch (Exception e) {
            log.error("加密失败", e);
            throw new EncryptionException("加密失败", e);
        }
    }
    
    /**
     * 解密字符串
     * 
     * @param ciphertext Base64编码的密文
     * @return 明文
     */
    public String decrypt(String ciphertext) {
        if (ciphertext == null) {
            return null;
        }
        
        try {
            // 解码 Base64
            byte[] decoded = Base64.getDecoder().decode(ciphertext);
            ByteBuffer buffer = ByteBuffer.wrap(decoded);
            
            // 读取 IV 长度
            short ivLength = buffer.getShort();
            
            // 读取 IV
            byte[] iv = new byte[ivLength];
            buffer.get(iv);
            
            // 读取密文
            byte[] encryptedData = new byte[buffer.remaining()];
            buffer.get(encryptedData);
            
            // 创建 GCM 参数规范
            GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            
            // 初始化解密器
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, gcmSpec);
            
            // 执行解密
            byte[] plaintext = cipher.doFinal(encryptedData);
            
            return new String(plaintext, StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.error("解密失败", e);
            throw new EncryptionException("解密失败", e);
        }
    }
    
    /**
     * 判断字符串是否已加密
     * 通过尝试解密来验证
     */
    public boolean isEncrypted(String value) {
        if (value == null || value.isEmpty()) {
            return false;
        }
        
        try {
            // 尝试 Base64 解码
            byte[] decoded = Base64.getDecoder().decode(value);
            
            // 检查格式: 至少要有 2字节(IV长度) + 12字节(IV) + 16字节(最小密文+标签)
            if (decoded.length < 30) {
                return false;
            }
            
            ByteBuffer buffer = ByteBuffer.wrap(decoded);
            short ivLength = buffer.getShort();
            
            // 验证 IV 长度
            return ivLength == GCM_IV_LENGTH;
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * 加密异常
     */
    public static class EncryptionException extends RuntimeException {
        public EncryptionException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
