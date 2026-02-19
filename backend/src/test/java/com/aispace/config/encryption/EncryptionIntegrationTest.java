package com.aispace.config.encryption;

import com.aispace.entity.Customer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

/**
 * 加密功能集成测试
 * 测试真实数据库环境下的加解密流程
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class EncryptionIntegrationTest {

    @Autowired
    private EncryptionService encryptionService;

    @PersistenceContext
    private EntityManager entityManager;

    @Test
    void customerEntity_shouldEncryptSensitiveFields() {
        // Given
        Customer customer = Customer.builder()
            .name("阿里巴巴集团")
            .shortName("阿里")
            .type(Customer.CustomerType.ENTERPRISE)
            .industry("互联网")
            .region("杭州")
            .address("浙江省杭州市西湖区文三路90号")
            .level(Customer.CustomerLevel.KEY)
            .relationshipScore(95)
            .notes("阿里巴巴是中国最大的电商平台，具有巨大的商业价值")
            .website("https://www.alibaba.com")
            .build();

        // When - 保存到数据库（触发加密）
        entityManager.persist(customer);
        entityManager.flush();
        entityManager.clear();

        // Then - 从数据库重新加载（触发解密）
        Customer loadedCustomer = entityManager.find(Customer.class, customer.getId());

        // 验证解密后的内容正确
        assertThat(loadedCustomer.getName()).isEqualTo("阿里巴巴集团");
        assertThat(loadedCustomer.getShortName()).isEqualTo("阿里");
        assertThat(loadedCustomer.getAddress()).isEqualTo("浙江省杭州市西湖区文三路90号");
        assertThat(loadedCustomer.getNotes()).isEqualTo("阿里巴巴是中国最大的电商平台，具有巨大的商业价值");

        // 验证数据库中存储的是加密数据
        String storedName = getRawFieldValue(customer.getId(), "name");
        String storedAddress = getRawFieldValue(customer.getId(), "address");
        
        assertThat(storedName).isNotEqualTo("阿里巴巴集团");
        assertThat(storedAddress).isNotEqualTo("浙江省杭州市西湖区文三路90号");
        assertThat(encryptionService.isEncrypted(storedName)).isTrue();
        assertThat(encryptionService.isEncrypted(storedAddress)).isTrue();
    }

    @Test
    void customerEntity_shouldHandleNullValues() {
        // Given
        Customer customer = Customer.builder()
            .name("测试客户")
            .type(Customer.CustomerType.ENTERPRISE)
            .level(Customer.CustomerLevel.NORMAL)
            .relationshipScore(50)
            .address(null)  // 故意设置为 null
            .notes(null)    // 故意设置为 null
            .build();

        // When
        entityManager.persist(customer);
        entityManager.flush();
        entityManager.clear();

        // Then
        Customer loadedCustomer = entityManager.find(Customer.class, customer.getId());
        
        assertThat(loadedCustomer.getName()).isEqualTo("测试客户");
        assertThat(loadedCustomer.getAddress()).isNull();
        assertThat(loadedCustomer.getNotes()).isNull();
    }

    @Test
    void customerEntity_shouldHandleEmptyStrings() {
        // Given
        Customer customer = Customer.builder()
            .name("测试客户")
            .type(Customer.CustomerType.ENTERPRISE)
            .level(Customer.CustomerLevel.NORMAL)
            .relationshipScore(50)
            .address("")    // 空字符串
            .notes(" ")     // 空白字符串
            .shortName("")  // 空字符串
            .build();

        // When
        entityManager.persist(customer);
        entityManager.flush();
        entityManager.clear();

        // Then
        Customer loadedCustomer = entityManager.find(Customer.class, customer.getId());
        
        assertThat(loadedCustomer.getName()).isEqualTo("测试客户");
        assertThat(loadedCustomer.getAddress()).isEqualTo("");
        assertThat(loadedCustomer.getNotes()).isEqualTo(" ");
        assertThat(loadedCustomer.getShortName()).isEqualTo("");
    }

    @Test
    void encryptionService_shouldMaintainConsistencyAcrossOperations() {
        // Given
        String originalText = "一致性测试文本 🚀";

        // When - 多次加密解密
        String encrypted1 = encryptionService.encrypt(originalText);
        String decrypted1 = encryptionService.decrypt(encrypted1);
        
        String encrypted2 = encryptionService.encrypt(originalText);
        String decrypted2 = encryptionService.decrypt(encrypted2);

        // Then
        assertThat(decrypted1).isEqualTo(originalText);
        assertThat(decrypted2).isEqualTo(originalText);
        assertThat(encrypted1).isNotEqualTo(encrypted2); // 每次加密结果不同
    }

    @Test
    void isEncrypted_shouldCorrectlyIdentifyEncryptedData() {
        // Given
        String plainText = "普通文本";
        String encryptedText = encryptionService.encrypt(plainText);

        // When & Then
        assertThat(encryptionService.isEncrypted(plainText)).isFalse();
        assertThat(encryptionService.isEncrypted(encryptedText)).isTrue();
        
        // 解密后的文本不应该被认为是加密的
        String decryptedText = encryptionService.decrypt(encryptedText);
        assertThat(encryptionService.isEncrypted(decryptedText)).isFalse();
    }

    @Test
    void customer_withSpecialCharacters_shouldWork() {
        // Given
        Customer customer = Customer.builder()
            .name("特殊字符测试公司!@#$%^&*()")
            .address("地址包含中文：北京市朝阳区😊")
            .notes("备注包含emoji: 🎉🎊🎈\n换行测试")
            .type(Customer.CustomerType.ENTERPRISE)
            .level(Customer.CustomerLevel.NORMAL)
            .relationshipScore(75)
            .build();

        // When
        entityManager.persist(customer);
        entityManager.flush();
        entityManager.clear();

        // Then
        Customer loadedCustomer = entityManager.find(Customer.class, customer.getId());
        
        assertThat(loadedCustomer.getName()).isEqualTo("特殊字符测试公司!@#$%^&*()");
        assertThat(loadedCustomer.getAddress()).isEqualTo("地址包含中文：北京市朝阳区😊");
        assertThat(loadedCustomer.getNotes()).isEqualTo("备注包含emoji: 🎉🎊🎈\n换行测试");
    }

    @Test
    void encryption_shouldWorkWithLongText() {
        // Given
        StringBuilder longNotes = new StringBuilder();
        for (int i = 0; i < 100; i++) {
            longNotes.append("这是第").append(i).append("行详细的客户备注信息，包含各种业务细节和联系方式。\n");
        }
        
        Customer customer = Customer.builder()
            .name("长文本测试客户")
            .notes(longNotes.toString())
            .type(Customer.CustomerType.ENTERPRISE)
            .level(Customer.CustomerLevel.NORMAL)
            .relationshipScore(80)
            .build();

        // When
        entityManager.persist(customer);
        entityManager.flush();
        entityManager.clear();

        // Then
        Customer loadedCustomer = entityManager.find(Customer.class, customer.getId());
        assertThat(loadedCustomer.getNotes()).isEqualTo(longNotes.toString());
    }

    // 辅助方法：直接从数据库获取原始字段值（绕过JPA的自动解密）
    private String getRawFieldValue(UUID customerId, String fieldName) {
        return (String) entityManager.createNativeQuery(
            "SELECT " + fieldName + " FROM customers WHERE id = ?")
            .setParameter(1, customerId)
            .getSingleResult();
    }
}