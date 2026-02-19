package com.aispace.config.encryption;

import com.aispace.entity.Customer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * 加密实体监听器测试类
 * 测试 EncryptionEntityListener 的加解密功能
 */
@SpringBootTest
@ActiveProfiles("test")
class EncryptionEntityListenerTest {

    @MockBean
    private EncryptionService encryptionService;

    private EncryptionEntityListener listener;

    @BeforeEach
    void setUp() {
        listener = new EncryptionEntityListener(encryptionService);
    }

    @Test
    void prePersist_shouldEncryptStringFields() {
        // Given
        Customer customer = Customer.builder()
            .name("张三科技有限公司")
            .address("北京市朝阳区某某大厦")
            .notes("重要客户，需要重点关注")
            .build();

        when(encryptionService.isEncrypted(anyString())).thenReturn(false);
        when(encryptionService.encrypt(anyString())).thenAnswer(invocation -> "encrypted_" + invocation.getArgument(0));

        // When
        listener.prePersist(customer);

        // Then
        assertThat(customer.getName()).startsWith("encrypted_");
        assertThat(customer.getAddress()).startsWith("encrypted_");
        assertThat(customer.getNotes()).startsWith("encrypted_");
        verify(encryptionService, times(3)).encrypt(anyString());
    }

    @Test
    void prePersist_shouldNotEncryptAlreadyEncryptedFields() {
        // Given
        Customer customer = Customer.builder()
            .name("encrypted_已加密数据")
            .address("北京市朝阳区某某大厦")
            .build();

        when(encryptionService.isEncrypted("encrypted_已加密数据")).thenReturn(true);
        when(encryptionService.isEncrypted("北京市朝阳区某某大厦")).thenReturn(false);
        when(encryptionService.encrypt("北京市朝阳区某某大厦")).thenReturn("encrypted_北京市朝阳区某某大厦");

        // When
        listener.prePersist(customer);

        // Then
        assertThat(customer.getName()).isEqualTo("encrypted_已加密数据"); // 保持不变
        assertThat(customer.getAddress()).startsWith("encrypted_");
        verify(encryptionService, times(1)).encrypt(anyString()); // 只加密一个字段
    }

    @Test
    void prePersist_shouldNotEncryptNonStringFields() {
        // Given
        Customer customer = Customer.builder()
            .name("张三科技有限公司")
            .type(Customer.CustomerType.ENTERPRISE)
            .level(Customer.CustomerLevel.KEY)
            .relationshipScore(90)
            .build();

        when(encryptionService.isEncrypted(anyString())).thenReturn(false);
        when(encryptionService.encrypt(anyString())).thenAnswer(invocation -> "encrypted_" + invocation.getArgument(0));

        // When
        listener.prePersist(customer);

        // Then
        assertThat(customer.getType()).isEqualTo(Customer.CustomerType.ENTERPRISE);
        assertThat(customer.getLevel()).isEqualTo(Customer.CustomerLevel.KEY);
        assertThat(customer.getRelationshipScore()).isEqualTo(90);
        verify(encryptionService, times(1)).encrypt(anyString()); // 只加密name字段
    }

    @Test
    void prePersist_shouldHandleNullEntity() {
        // When & Then
        assertThatCode(() -> listener.prePersist(null)).doesNotThrowAnyException();
    }

    @Test
    void prePersist_shouldHandleNullStringValues() {
        // Given
        Customer customer = Customer.builder()
            .name(null)
            .address(null)
            .notes(null)
            .build();

        // When & Then
        assertThatCode(() -> listener.prePersist(customer)).doesNotThrowAnyException();
        verify(encryptionService, never()).encrypt(anyString());
    }

    @Test
    void prePersist_shouldHandleEmptyStringValues() {
        // Given
        Customer customer = Customer.builder()
            .name("")
            .address(" ")
            .notes("有效备注")
            .build();

        when(encryptionService.isEncrypted("")).thenReturn(false);
        when(encryptionService.isEncrypted(" ")).thenReturn(false);
        when(encryptionService.isEncrypted("有效备注")).thenReturn(false);
        when(encryptionService.encrypt("有效备注")).thenReturn("encrypted_有效备注");

        // When
        listener.prePersist(customer);

        // Then
        assertThat(customer.getName()).isEqualTo(""); // 空字符串不加密
        assertThat(customer.getAddress()).isEqualTo(" "); // 空白字符串不加密
        assertThat(customer.getNotes()).startsWith("encrypted_");
        verify(encryptionService, times(1)).encrypt(anyString());
    }

    @Test
    void postLoad_shouldDecryptStringFields() {
        // Given
        Customer customer = Customer.builder()
            .name("encrypted_张三科技有限公司")
            .address("encrypted_北京市朝阳区某某大厦")
            .notes("encrypted_重要客户备注")
            .build();

        when(encryptionService.isEncrypted(anyString())).thenReturn(true);
        when(encryptionService.decrypt(anyString())).thenAnswer(invocation -> 
            invocation.getArgument(0).toString().replace("encrypted_", ""));

        // When
        listener.postLoad(customer);

        // Then
        assertThat(customer.getName()).isEqualTo("张三科技有限公司");
        assertThat(customer.getAddress()).isEqualTo("北京市朝阳区某某大厦");
        assertThat(customer.getNotes()).isEqualTo("重要客户备注");
        verify(encryptionService, times(3)).decrypt(anyString());
    }

    @Test
    void postLoad_shouldNotDecryptNonEncryptedFields() {
        // Given
        Customer customer = Customer.builder()
            .name("明文公司名称")
            .address("encrypted_北京市朝阳区某某大厦")
            .notes("another_plain_text")
            .build();

        when(encryptionService.isEncrypted("明文公司名称")).thenReturn(false);
        when(encryptionService.isEncrypted("encrypted_北京市朝阳区某某大厦")).thenReturn(true);
        when(encryptionService.isEncrypted("another_plain_text")).thenReturn(false);
        when(encryptionService.decrypt("encrypted_北京市朝阳区某某大厦"))
            .thenReturn("北京市朝阳区某某大厦");

        // When
        listener.postLoad(customer);

        // Then
        assertThat(customer.getName()).isEqualTo("明文公司名称"); // 保持不变
        assertThat(customer.getAddress()).isEqualTo("北京市朝阳区某某大厦");
        assertThat(customer.getNotes()).isEqualTo("another_plain_text"); // 保持不变
        verify(encryptionService, times(1)).decrypt(anyString());
    }

    @Test
    void postLoad_shouldHandleDecryptionFailureGracefully() {
        // Given
        Customer customer = Customer.builder()
            .name("encrypted_corrupted_data")
            .address("正常地址")
            .build();

        when(encryptionService.isEncrypted("encrypted_corrupted_data")).thenReturn(true);
        when(encryptionService.isEncrypted("正常地址")).thenReturn(false);
        when(encryptionService.decrypt("encrypted_corrupted_data"))
            .thenThrow(new EncryptionService.EncryptionException("解密失败", new RuntimeException()));

        // When
        listener.postLoad(customer);

        // Then
        assertThat(customer.getName()).isEqualTo("encrypted_corrupted_data"); // 保持原值
        assertThat(customer.getAddress()).isEqualTo("正常地址");
        verify(encryptionService, times(1)).decrypt(anyString());
    }

    @Test
    void postLoad_shouldHandleNullEntity() {
        // When & Then
        assertThatCode(() -> listener.postLoad(null)).doesNotThrowAnyException();
    }

    @Test
    void postLoad_shouldHandleNullStringValues() {
        // Given
        Customer customer = Customer.builder()
            .name(null)
            .address(null)
            .notes(null)
            .build();

        // When & Then
        assertThatCode(() -> listener.postLoad(customer)).doesNotThrowAnyException();
        verify(encryptionService, never()).decrypt(anyString());
    }

    @Test
    void encryptEntity_shouldHandleJsonFields() {
        // Given
        Map<String, Object> orgStructure = new HashMap<>();
        orgStructure.put("departments", new String[]{"销售部", "技术部"});
        orgStructure.put("employees", 100);

        Customer customer = Customer.builder()
            .name("测试公司")
            .orgStructure(orgStructure)
            .build();

        when(encryptionService.isEncrypted(anyString())).thenReturn(false);
        when(encryptionService.encrypt(anyString())).thenAnswer(invocation -> "encrypted_" + invocation.getArgument(0));

        // When
        listener.encryptEntity(customer);

        // Then
        assertThat(customer.getName()).startsWith("encrypted_");
        // orgStructure 是 Map 类型，不会被加密
        assertThat(customer.getOrgStructure()).isEqualTo(orgStructure);
        verify(encryptionService, times(1)).encrypt(anyString());
    }

    @Test
    void decryptEntity_shouldHandleJsonFields() {
        // Given
        Map<String, Object> orgStructure = new HashMap<>();
        orgStructure.put("departments", new String[]{"销售部", "技术部"});

        Customer customer = Customer.builder()
            .name("encrypted_测试公司")
            .orgStructure(orgStructure)
            .build();

        when(encryptionService.isEncrypted("encrypted_测试公司")).thenReturn(true);
        when(encryptionService.isEncrypted(anyString())).thenReturn(false);
        when(encryptionService.decrypt("encrypted_测试公司")).thenReturn("测试公司");

        // When
        listener.decryptEntity(customer);

        // Then
        assertThat(customer.getName()).isEqualTo("测试公司");
        // orgStructure 保持不变
        assertThat(customer.getOrgStructure()).isEqualTo(orgStructure);
        verify(encryptionService, times(1)).decrypt(anyString());
    }

    @Test
    void getEncryptedFields_shouldCacheResults() {
        // Given
        Customer customer1 = new Customer();
        Customer customer2 = new Customer();

        // When
        listener.encryptEntity(customer1);
        listener.encryptEntity(customer2);

        // Then - 第二次调用应该使用缓存，提高性能
        // 这里主要是验证不会抛异常
        assertThatCode(() -> {
            listener.encryptEntity(customer1);
            listener.encryptEntity(customer2);
        }).doesNotThrowAnyException();
    }
}