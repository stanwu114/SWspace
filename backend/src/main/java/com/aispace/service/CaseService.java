package com.aispace.service;

import com.aispace.dto.response.PageResponse;
import com.aispace.entity.Case;
import com.aispace.entity.Knowledge;
import com.aispace.repository.CaseRepository;
import com.aispace.repository.KnowledgeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 案例管理服务
 * 处理案例的CRUD、发布、知识提取等
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CaseService {
    
    private final CaseRepository caseRepository;
    private final KnowledgeRepository knowledgeRepository;
    
    /**
     * 获取案例列表
     */
    public PageResponse<Case> listCases(
            String industry,
            String region,
            Case.CustomerType customerType,
            Case.CaseStatus status,
            String keyword,
            int page,
            int pageSize) {
        
        Pageable pageable = PageRequest.of(page - 1, pageSize, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Case> result;
        
        if (keyword != null && !keyword.isBlank()) {
            result = caseRepository.searchByKeyword(keyword, pageable);
        } else if (status != null) {
            result = caseRepository.findByStatus(status, pageable);
        } else {
            result = caseRepository.searchCases(industry, region, customerType, pageable);
        }
        
        return PageResponse.<Case>builder()
            .items(result.getContent())
            .total(result.getTotalElements())
            .page(page)
            .pageSize(pageSize)
            .totalPages(result.getTotalPages())
            .build();
    }
    
    /**
     * 获取案例详情
     */
    @Transactional
    public Optional<Case> getCase(UUID id, boolean incrementView) {
        Optional<Case> caseOpt = caseRepository.findById(id);
        if (caseOpt.isPresent() && incrementView) {
            caseRepository.incrementViewCount(id);
        }
        return caseOpt;
    }
    
    /**
     * 创建案例
     */
    @Transactional
    public Case createCase(Case caseEntity) {
        log.info("Creating case: {}", caseEntity.getTitle());
        return caseRepository.save(caseEntity);
    }
    
    /**
     * 更新案例
     */
    @Transactional
    public Case updateCase(UUID id, Case updates) {
        Case existing = caseRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Case not found: " + id));
        
        if (updates.getTitle() != null) existing.setTitle(updates.getTitle());
        if (updates.getDescription() != null) existing.setDescription(updates.getDescription());
        if (updates.getCustomerName() != null) existing.setCustomerName(updates.getCustomerName());
        if (updates.getCustomerType() != null) existing.setCustomerType(updates.getCustomerType());
        if (updates.getIndustry() != null) existing.setIndustry(updates.getIndustry());
        if (updates.getRegion() != null) existing.setRegion(updates.getRegion());
        if (updates.getContractValue() != null) existing.setContractValue(updates.getContractValue());
        if (updates.getStartDate() != null) existing.setStartDate(updates.getStartDate());
        if (updates.getEndDate() != null) existing.setEndDate(updates.getEndDate());
        if (updates.getBackground() != null) existing.setBackground(updates.getBackground());
        if (updates.getChallenge() != null) existing.setChallenge(updates.getChallenge());
        if (updates.getSolution() != null) existing.setSolution(updates.getSolution());
        if (updates.getOutcome() != null) existing.setOutcome(updates.getOutcome());
        if (updates.getKeyHighlights() != null) existing.setKeyHighlights(updates.getKeyHighlights());
        if (updates.getTechStack() != null) existing.setTechStack(updates.getTechStack());
        if (updates.getTags() != null) existing.setTags(updates.getTags());
        if (updates.getMetrics() != null) existing.setMetrics(updates.getMetrics());
        if (updates.getLessonsLearned() != null) existing.setLessonsLearned(updates.getLessonsLearned());
        
        log.info("Updating case: {}", id);
        return caseRepository.save(existing);
    }
    
    /**
     * 删除案例
     */
    @Transactional
    public void deleteCase(UUID id) {
        log.info("Deleting case: {}", id);
        caseRepository.deleteById(id);
    }
    
    /**
     * 发布案例
     */
    @Transactional
    public Case publishCase(UUID id) {
        Case caseEntity = caseRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Case not found: " + id));
        
        caseEntity.setStatus(Case.CaseStatus.PUBLISHED);
        log.info("Publishing case: {}", id);
        
        // 自动提取知识到知识库
        extractKnowledgeFromCase(caseEntity);
        
        return caseRepository.save(caseEntity);
    }
    
    /**
     * 归档案例
     */
    @Transactional
    public Case archiveCase(UUID id) {
        Case caseEntity = caseRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Case not found: " + id));
        
        caseEntity.setStatus(Case.CaseStatus.ARCHIVED);
        log.info("Archiving case: {}", id);
        return caseRepository.save(caseEntity);
    }
    
    /**
     * 设置精选
     */
    @Transactional
    public Case toggleFeatured(UUID id) {
        Case caseEntity = caseRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Case not found: " + id));
        
        caseEntity.setIsFeatured(!caseEntity.getIsFeatured());
        return caseRepository.save(caseEntity);
    }
    
    /**
     * 获取精选案例
     */
    public List<Case> getFeaturedCases(int limit) {
        return caseRepository.findFeaturedCases(PageRequest.of(0, limit));
    }
    
    /**
     * 记录引用
     */
    @Transactional
    public void recordReference(UUID id) {
        caseRepository.incrementReferenceCount(id);
    }
    
    /**
     * 从项目创建案例
     */
    @Transactional
    public Case createFromProject(UUID projectId, String author) {
        // 这里应该从项目获取信息来创建案例
        Case caseEntity = Case.builder()
            .projectId(projectId)
            .status(Case.CaseStatus.DRAFT)
            .author(author)
            .build();
        
        return caseRepository.save(caseEntity);
    }
    
    /**
     * 获取项目关联的案例
     */
    public List<Case> getCasesByProject(UUID projectId) {
        return caseRepository.findByProjectId(projectId);
    }
    
    /**
     * 从案例提取知识
     */
    @Transactional
    public List<Knowledge> extractKnowledgeFromCase(Case caseEntity) {
        List<Knowledge> extractedKnowledge = new ArrayList<>();
        
        // 提取解决方案知识
        if (caseEntity.getSolution() != null && !caseEntity.getSolution().isBlank()) {
            Knowledge solutionKnowledge = Knowledge.builder()
                .title(caseEntity.getTitle() + " - 解决方案")
                .content(caseEntity.getSolution())
                .summary(truncate(caseEntity.getSolution(), 500))
                .category(Knowledge.KnowledgeCategory.SOLUTION)
                .sourceType(Knowledge.KnowledgeSourceType.CASE)
                .sourceId(caseEntity.getId())
                .sourceName(caseEntity.getTitle())
                .tags(caseEntity.getTags())
                .keywords(caseEntity.getTechStack())
                .build();
            extractedKnowledge.add(knowledgeRepository.save(solutionKnowledge));
        }
        
        // 提取经验教训
        if (caseEntity.getLessonsLearned() != null && !caseEntity.getLessonsLearned().isEmpty()) {
            String lessonsContent = String.join("\n", caseEntity.getLessonsLearned());
            Knowledge lessonKnowledge = Knowledge.builder()
                .title(caseEntity.getTitle() + " - 经验教训")
                .content(lessonsContent)
                .summary(truncate(lessonsContent, 500))
                .category(Knowledge.KnowledgeCategory.LESSON)
                .sourceType(Knowledge.KnowledgeSourceType.CASE)
                .sourceId(caseEntity.getId())
                .sourceName(caseEntity.getTitle())
                .tags(caseEntity.getTags())
                .build();
            extractedKnowledge.add(knowledgeRepository.save(lessonKnowledge));
        }
        
        // 提取案例本身
        Knowledge caseKnowledge = Knowledge.builder()
            .title(caseEntity.getTitle())
            .content(buildCaseContent(caseEntity))
            .summary(caseEntity.getDescription())
            .category(Knowledge.KnowledgeCategory.CASE)
            .sourceType(Knowledge.KnowledgeSourceType.CASE)
            .sourceId(caseEntity.getId())
            .sourceName(caseEntity.getTitle())
            .tags(caseEntity.getTags())
            .keywords(caseEntity.getTechStack())
            .build();
        extractedKnowledge.add(knowledgeRepository.save(caseKnowledge));
        
        log.info("Extracted {} knowledge items from case: {}", extractedKnowledge.size(), caseEntity.getId());
        return extractedKnowledge;
    }
    
    /**
     * 获取统计信息
     */
    public Map<String, Object> getStatistics() {
        Map<String, Object> stats = new HashMap<>();
        
        stats.put("total", caseRepository.count());
        stats.put("published", caseRepository.findByStatus(Case.CaseStatus.PUBLISHED, 
            PageRequest.of(0, 1)).getTotalElements());
        stats.put("draft", caseRepository.findByStatus(Case.CaseStatus.DRAFT, 
            PageRequest.of(0, 1)).getTotalElements());
        
        // 按行业统计
        List<Object[]> industryStats = caseRepository.countByIndustry();
        Map<String, Long> byIndustry = industryStats.stream()
            .collect(Collectors.toMap(
                r -> r[0] != null ? (String) r[0] : "未分类",
                r -> (Long) r[1]
            ));
        stats.put("byIndustry", byIndustry);
        
        return stats;
    }
    
    /**
     * 获取可用筛选项
     */
    public Map<String, List<String>> getFilterOptions() {
        Map<String, List<String>> options = new HashMap<>();
        options.put("industries", caseRepository.findDistinctIndustries());
        options.put("regions", caseRepository.findDistinctRegions());
        return options;
    }
    
    private String buildCaseContent(Case caseEntity) {
        StringBuilder sb = new StringBuilder();
        
        if (caseEntity.getBackground() != null) {
            sb.append("## 项目背景\n").append(caseEntity.getBackground()).append("\n\n");
        }
        if (caseEntity.getChallenge() != null) {
            sb.append("## 挑战\n").append(caseEntity.getChallenge()).append("\n\n");
        }
        if (caseEntity.getSolution() != null) {
            sb.append("## 解决方案\n").append(caseEntity.getSolution()).append("\n\n");
        }
        if (caseEntity.getOutcome() != null) {
            sb.append("## 成果\n").append(caseEntity.getOutcome()).append("\n\n");
        }
        
        return sb.toString();
    }
    
    private String truncate(String text, int maxLength) {
        if (text == null || text.length() <= maxLength) {
            return text;
        }
        return text.substring(0, maxLength) + "...";
    }
}
