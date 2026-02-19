package com.aispace.config.encryption;

import com.aispace.entity.Contact;
import com.aispace.entity.Customer;
import com.aispace.entity.Document;
import com.aispace.entity.Interaction;
import com.aispace.entity.Project;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.*;

/**
 * 多实体加密综合测试
 * 测试所有带有 @Encrypted 注解的实体类型
 */
@DataJpaTest
@ActiveProfiles("test")
@Import({EncryptionService.class, EncryptionEntityListener.class})
class MultiEntityEncryptionTest {

    @Autowired
    private EncryptionService encryptionService;

    @Autowired
    private TestEntityManager entityManager;

    @Test
    void customerEntity_shouldEncryptAllSensitiveFields() {
        // Given
        Customer customer = Customer.builder()
            .name("华为技术有限公司")
            .shortName("华为")
            .type(Customer.CustomerType.ENTERPRISE)
            .industry("通信设备")
            .region("深圳")
            .address("广东省深圳市龙岗区坂田华为总部办公楼")
            .level(Customer.CustomerLevel.KEY)
            .relationshipScore(98)
            .notes("全球领先的ICT基础设施和智能终端提供商")
            .website("https://www.huawei.com")
            .build();

        // When
        Customer saved = entityManager.persistAndFlush(customer);
        entityManager.clear();

        // Then
        Customer loaded = entityManager.find(Customer.class, saved.getId());
        assertCustomerFieldsDecrypted(loaded, customer);
    }

    @Test
    void contactEntity_shouldEncryptAllSensitiveFields() {
        // Given
        Customer customer = createTestCustomer();
        Customer savedCustomer = entityManager.persistAndFlush(customer);

        Contact contact = Contact.builder()
            .customerId(savedCustomer.getId())
            .name("李明")
            .title("采购总监")
            .department("采购部")
            .role(Contact.ContactRole.DECISION_MAKER)
            .importance(Contact.ContactImportance.KEY)
            .phone("0755-12345678")
            .mobile("13800138000")
            .email("liming@huawei.com")
            .wechat("liming_wechat")
            .birthday(LocalDate.of(1985, 6, 15))
            .notes("负责公司采购决策的关键人物")
            .isActive(true)
            .build();

        // When
        Contact saved = entityManager.persistAndFlush(contact);
        entityManager.clear();

        // Then
        Contact loaded = entityManager.find(Contact.class, saved.getId());
        assertContactFieldsDecrypted(loaded, contact);
    }

    @Test
    void projectEntity_shouldEncryptAllSensitiveFields() {
        // Given
        Customer customer = createTestCustomer();
        Customer savedCustomer = entityManager.persistAndFlush(customer);

        Map<String, Object> requirements = new HashMap<>();
        requirements.put("mainFeatures", Arrays.asList("云服务", "大数据分析", "AI智能推荐"));
        requirements.put("techStack", Arrays.asList("Java", "Spring Boot", "React"));

        Map<String, Object> riskAssessment = new HashMap<>();
        riskAssessment.put("level", "medium");
        riskAssessment.put("factors", Arrays.asList("市场竞争激烈", "技术难度较高"));

        Project project = Project.builder()
            .name("华为云服务平台建设项目")
            .code("HW-YF-2024-001")
            .customerId(savedCustomer.getId())
            .status(Project.ProjectStatus.OPPORTUNITY)
            .stage("需求分析阶段")
            .estimatedValue(java.math.BigDecimal.valueOf(5000000))
            .description("为华为构建新一代云服务平台，提供弹性计算、存储和网络服务")
            .requirements(requirements)
            .riskAssessment(riskAssessment)
            .tags(Arrays.asList("云计算", "企业服务", "数字化转型"))
            .build();

        // When
        Project saved = entityManager.persistAndFlush(project);
        entityManager.clear();

        // Then
        Project loaded = entityManager.find(Project.class, saved.getId());
        assertProjectFieldsDecrypted(loaded, project);
    }

    @Test
    void documentEntity_shouldEncryptAllSensitiveFields() {
        // Given
        Document document = Document.builder()
            .name("华为云平台技术方案.pdf")
            .originalName("Huawei_Cloud_Platform_Technical_Specification.pdf")
            .type(Document.DocumentType.SOLUTION)
            .filePath("/documents/solutions/huawei_cloud_platform.pdf")
            .fileSize(2048000L)
            .fileExt("pdf")
            .mimeType("application/pdf")
            .contentText("这是华为云平台的详细技术方案文档，包含了架构设计、技术选型、实施计划等内容...")
            .aiSummary("本文档详细描述了华为云平台的技术架构和实施方案，采用微服务架构...")
            .version(1)
            .isLatest(true)
            .tags(Arrays.asList("技术方案", "云平台", "华为"))
            .build();

        // When
        Document saved = entityManager.persistAndFlush(document);
        entityManager.clear();

        // Then
        Document loaded = entityManager.find(Document.class, saved.getId());
        assertDocumentFieldsDecrypted(loaded, document);
    }

    @Test
    void interactionEntity_shouldEncryptAllSensitiveFields() {
        // Given
        Customer customer = createTestCustomer();
        Customer savedCustomer = entityManager.persistAndFlush(customer);

        Interaction interaction = Interaction.builder()
            .customerId(savedCustomer.getId())
            .type(Interaction.InteractionType.MEETING)
            .subject("华为云平台合作洽谈会议")
            .content("今日与华为采购总监李明进行了深入的商务洽谈，讨论了云平台建设项目的具体需求和技术方案...")
            .summary("会议达成初步合作意向，华为对我们的技术方案表示认可，下一步将进行POC测试")
            .location("华为深圳总部会议室A")
            .interactionAt(LocalDateTime.now())
            .duration(120)
            .build();

        // When
        Interaction saved = entityManager.persistAndFlush(interaction);
        entityManager.clear();

        // Then
        Interaction loaded = entityManager.find(Interaction.class, saved.getId());
        assertInteractionFieldsDecrypted(loaded, interaction);
    }

    @Test
    void mixedEntities_shouldWorkIndependently() {
        // Given - 创建多个不同类型的相关实体
        Customer customer = Customer.builder()
            .name("腾讯科技")
            .type(Customer.CustomerType.ENTERPRISE)
            .industry("互联网")
            .region("深圳")
            .address("深圳市南山区科技园")
            .notes("中国最大的互联网公司之一")
            .build();

        Contact contact = Contact.builder()
            .name("马化腾")
            .title("CEO")
            .email("pony@tencent.com")
            .phone("0755-88888888")
            .build();

        Project project = Project.builder()
            .name("腾讯游戏平台优化项目")
            .description("优化腾讯游戏平台的性能和用户体验")
            .build();

        Document document = Document.builder()
            .name("腾讯项目提案.docx")
            .contentText("这是给腾讯的游戏平台优化提案...")
            .aiSummary("提案概述了针对腾讯游戏平台的优化方案")
            .build();

        Interaction interaction = Interaction.builder()
            .type(Interaction.InteractionType.EMAIL)
            .subject("腾讯项目提案发送")
            .content("已将项目提案通过邮件发送给腾讯相关负责人")
            .build();

        // When
        Customer savedCustomer = entityManager.persistAndFlush(customer);
        contact.setCustomerId(savedCustomer.getId());
        Contact savedContact = entityManager.persistAndFlush(contact);
        project.setCustomerId(savedCustomer.getId());
        Project savedProject = entityManager.persistAndFlush(project);
        document.setCustomerId(savedCustomer.getId());
        Document savedDocument = entityManager.persistAndFlush(document);
        interaction.setCustomerId(savedCustomer.getId());
        Interaction savedInteraction = entityManager.persistAndFlush(interaction);
        
        entityManager.clear();

        // Then - 验证所有实体都能正确加解密
        Customer loadedCustomer = entityManager.find(Customer.class, savedCustomer.getId());
        Contact loadedContact = entityManager.find(Contact.class, savedContact.getId());
        Project loadedProject = entityManager.find(Project.class, savedProject.getId());
        Document loadedDocument = entityManager.find(Document.class, savedDocument.getId());
        Interaction loadedInteraction = entityManager.find(Interaction.class, savedInteraction.getId());

        assertThat(loadedCustomer.getName()).isEqualTo("腾讯科技");
        assertThat(loadedContact.getName()).isEqualTo("马化腾");
        assertThat(loadedProject.getDescription()).contains("腾讯游戏平台");
        assertThat(loadedDocument.getContentText()).contains("腾讯");
        assertThat(loadedInteraction.getContent()).contains("腾讯");
    }

    // 辅助方法
    private Customer createTestCustomer() {
        return Customer.builder()
            .name("测试客户公司")
            .type(Customer.CustomerType.ENTERPRISE)
            .level(Customer.CustomerLevel.NORMAL)
            .relationshipScore(50)
            .build();
    }

    private void assertCustomerFieldsDecrypted(Customer actual, Customer expected) {
        assertThat(actual.getName()).isEqualTo(expected.getName());
        assertThat(actual.getShortName()).isEqualTo(expected.getShortName());
        assertThat(actual.getAddress()).isEqualTo(expected.getAddress());
        assertThat(actual.getNotes()).isEqualTo(expected.getNotes());
    }

    private void assertContactFieldsDecrypted(Contact actual, Contact expected) {
        assertThat(actual.getName()).isEqualTo(expected.getName());
        assertThat(actual.getPhone()).isEqualTo(expected.getPhone());
        assertThat(actual.getMobile()).isEqualTo(expected.getMobile());
        assertThat(actual.getEmail()).isEqualTo(expected.getEmail());
        assertThat(actual.getWechat()).isEqualTo(expected.getWechat());
        assertThat(actual.getNotes()).isEqualTo(expected.getNotes());
    }

    private void assertProjectFieldsDecrypted(Project actual, Project expected) {
        assertThat(actual.getDescription()).isEqualTo(expected.getDescription());
        assertThat(actual.getRequirements()).isEqualTo(expected.getRequirements());
        assertThat(actual.getRiskAssessment()).isEqualTo(expected.getRiskAssessment());
    }

    private void assertDocumentFieldsDecrypted(Document actual, Document expected) {
        assertThat(actual.getContentText()).isEqualTo(expected.getContentText());
        assertThat(actual.getAiAnalysis()).isEqualTo(expected.getAiAnalysis());
        assertThat(actual.getAiSummary()).isEqualTo(expected.getAiSummary());
    }

    private void assertInteractionFieldsDecrypted(Interaction actual, Interaction expected) {
        assertThat(actual.getContent()).isEqualTo(expected.getContent());
        assertThat(actual.getSummary()).isEqualTo(expected.getSummary());
        assertThat(actual.getLocation()).isEqualTo(expected.getLocation());
    }
}