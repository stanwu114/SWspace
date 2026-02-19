package com.aispace.config.encryption;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.*;

/**
 * 加密服务测试类
 * 测试 EncryptionService 的核心功能
 */
class EncryptionServiceTest {

    private EncryptionService encryptionService;

    @BeforeEach
    void setUp() {
        encryptionService = new EncryptionService();
        // 设置测试密钥
        ReflectionTestUtils.setField(encryptionService, "encryptionKey", "test-secret-key-for-unit-tests-12345");
        ReflectionTestUtils.setField(encryptionService, "strictKeyValidation", false);
        ReflectionTestUtils.setField(encryptionService, "activeProfile", "test");
        
        // 初始化服务
        encryptionService.init();
    }

    @Test
    void encrypt_shouldEncryptStringSuccessfully() {
        // Given
        String plaintext = "这是一条测试消息";

        // When
        String encrypted = encryptionService.encrypt(plaintext);

        // Then
        assertThat(encrypted).isNotNull();
        assertThat(encrypted).isNotBlank();
        assertThat(encrypted).isNotEqualTo(plaintext);
        // 验证是 Base64 格式
        assertThat(encrypted).matches("^[A-Za-z0-9+/]*={0,2}$");
    }

    @Test
    void decrypt_shouldDecryptStringSuccessfully() {
        // Given
        String plaintext = "Hello World! 你好世界！";
        String encrypted = encryptionService.encrypt(plaintext);

        // When
        String decrypted = encryptionService.decrypt(encrypted);

        // Then
        assertThat(decrypted).isEqualTo(plaintext);
    }

    @Test
    void encrypt_decrypt_shouldMaintainDataIntegrity() {
        // Given
        String originalText = "这是一个包含特殊字符的测试文本: !@#$%^&*()_+-=[]{}|;':\",./<>?中文测试";

        // When
        String encrypted = encryptionService.encrypt(originalText);
        String decrypted = encryptionService.decrypt(encrypted);

        // Then
        assertThat(decrypted).isEqualTo(originalText);
    }

    @Test
    void encrypt_samePlaintext_shouldProduceDifferentCiphertext() {
        // Given
        String plaintext = "相同明文";

        // When
        String encrypted1 = encryptionService.encrypt(plaintext);
        String encrypted2 = encryptionService.encrypt(plaintext);

        // Then
        assertThat(encrypted1).isNotEqualTo(encrypted2);
        // 但解密后应该相同
        assertThat(encryptionService.decrypt(encrypted1)).isEqualTo(plaintext);
        assertThat(encryptionService.decrypt(encrypted2)).isEqualTo(plaintext);
    }

    @Test
    void isEncrypted_shouldReturnTrueForEncryptedString() {
        // Given
        String plaintext = "测试文本";
        String encrypted = encryptionService.encrypt(plaintext);

        // When & Then
        assertThat(encryptionService.isEncrypted(encrypted)).isTrue();
    }

    @Test
    void isEncrypted_shouldReturnFalseForPlainString() {
        // Given
        String plaintext = "普通文本";

        // When & Then
        assertThat(encryptionService.isEncrypted(plaintext)).isFalse();
    }

    @Test
    void isEncrypted_shouldReturnFalseForNull() {
        // When & Then
        assertThat(encryptionService.isEncrypted(null)).isFalse();
    }

    @Test
    void isEncrypted_shouldReturnFalseForEmptyString() {
        // When & Then
        assertThat(encryptionService.isEncrypted("")).isFalse();
        assertThat(encryptionService.isEncrypted("   ")).isFalse();
    }

    @Test
    void encrypt_shouldHandleNullInput() {
        // When
        String result = encryptionService.encrypt(null);

        // Then
        assertThat(result).isNull();
    }

    @Test
    void decrypt_shouldHandleNullInput() {
        // When
        String result = encryptionService.decrypt(null);

        // Then
        assertThat(result).isNull();
    }

    @Test
    void decrypt_shouldThrowExceptionForInvalidCiphertext() {
        // Given
        String invalidCiphertext = "invalid-base64-string";

        // When & Then
        assertThatThrownBy(() -> encryptionService.decrypt(invalidCiphertext))
            .isInstanceOf(EncryptionService.EncryptionException.class)
            .hasMessageContaining("解密失败");
    }

    @Test
    void decrypt_shouldThrowExceptionForTamperedCiphertext() {
        // Given
        String original = encryptionService.encrypt("原始文本");
        // 修改密文最后几个字符（模拟篡改）
        String tampered = original.substring(0, original.length() - 4) + "AAAA";

        // When & Then
        assertThatThrownBy(() -> encryptionService.decrypt(tampered))
            .isInstanceOf(EncryptionService.EncryptionException.class)
            .hasMessageContaining("解密失败");
    }

    @Test
    void encrypt_emptyString_shouldWork() {
        // Given
        String empty = "";

        // When
        String encrypted = encryptionService.encrypt(empty);
        String decrypted = encryptionService.decrypt(encrypted);

        // Then
        assertThat(decrypted).isEqualTo(empty);
    }

    @Test
    void encrypt_specialCharacters_shouldWork() {
        // Given
        String specialChars = "!@#$%^&*()_+-=[]{}|;':\",./<>?`~中文测试🎉";

        // When
        String encrypted = encryptionService.encrypt(specialChars);
        String decrypted = encryptionService.decrypt(encrypted);

        // Then
        assertThat(decrypted).isEqualTo(specialChars);
    }

    @Test
    void encrypt_longText_shouldWork() {
        // Given
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 1000; i++) {
            sb.append("这是第").append(i).append("行文本。\n");
        }
        String longText = sb.toString();

        // When
        String encrypted = encryptionService.encrypt(longText);
        String decrypted = encryptionService.decrypt(encrypted);

        // Then
        assertThat(decrypted).isEqualTo(longText);
    }

    @Test
    void isEncrypted_shouldReturnFalseForInvalidBase64() {
        // Given
        String invalidBase64 = "这不是有效的Base64编码";

        // When & Then
        assertThat(encryptionService.isEncrypted(invalidBase64)).isFalse();
    }

    @Test
    void isEncrypted_shouldReturnFalseForTooShortString() {
        // Given
        String shortString = "短";

        // When & Then
        assertThat(encryptionService.isEncrypted(shortString)).isFalse();
    }

    @Test
    void encrypt_unicodeCharacters_shouldWork() {
        // Given
        String unicodeText = "🌍🌎🌏🚀✨🎉🎊🎈🎁💎💡🔥💧🌱🌿🍀🌻";

        // When
        String encrypted = encryptionService.encrypt(unicodeText);
        String decrypted = encryptionService.decrypt(encrypted);

        // Then
        assertThat(decrypted).isEqualTo(unicodeText);
    }
}