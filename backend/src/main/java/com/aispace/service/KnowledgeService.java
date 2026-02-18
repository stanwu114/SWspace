package com.aispace.service;

import com.aispace.dto.response.PageResponse;
import com.aispace.entity.Knowledge;
import com.aispace.repository.KnowledgeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 知识库服务层
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class KnowledgeService {
    
    private final KnowledgeRepository knowledgeRepository;
    
    /**
     * 获取知识列表（分页）
     */
    public PageResponse<Knowledge> listKnowledge(
            Knowledge.KnowledgeCategory category,
            String subcategory,
            String keyword,
            int page,
            int pageSize,
            String sortBy,
            String sortOrder
    ) {
        Sort sort = Sort.by(
            "desc".equalsIgnoreCase(sortOrder) ? Sort.Direction.DESC : Sort.Direction.ASC,
            sortBy != null ? sortBy : "createdAt"
        );
        Pageable pageable = PageRequest.of(page - 1, pageSize, sort);
        
        Page<Knowledge> result;
        
        if (keyword != null && !keyword.isBlank()) {
            result = knowledgeRepository.searchByKeyword(keyword, pageable);
        } else if (category != null && subcategory != null && !subcategory.isBlank()) {
            result = knowledgeRepository.findByCategoryAndSubcategory(category, subcategory, pageable);
        } else if (category != null) {
            result = knowledgeRepository.findByCategory(category, pageable);
        } else {
            result = knowledgeRepository.findAll(pageable);
        }
        
        return PageResponse.of(
            result.getContent(),
            result.getTotalElements(),
            page,
            pageSize
        );
    }
    
    /**
     * 获取知识详情
     */
    @Transactional
    public Optional<Knowledge> getKnowledge(UUID id, boolean incrementView) {
        Optional<Knowledge> knowledge = knowledgeRepository.findById(id);
        if (knowledge.isPresent() && incrementView) {
            knowledgeRepository.incrementViewCount(id);
        }
        return knowledge;
    }
    
    /**
     * 创建知识
     */
    @Transactional
    public Knowledge createKnowledge(Knowledge knowledge) {
        log.info("Creating knowledge: {}", knowledge.getTitle());
        return knowledgeRepository.save(knowledge);
    }
    
    /**
     * 更新知识
     */
    @Transactional
    public Knowledge updateKnowledge(UUID id, Knowledge knowledgeData) {
        Knowledge knowledge = knowledgeRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Knowledge not found: " + id));
        
        if (knowledgeData.getTitle() != null) {
            knowledge.setTitle(knowledgeData.getTitle());
        }
        if (knowledgeData.getContent() != null) {
            knowledge.setContent(knowledgeData.getContent());
        }
        if (knowledgeData.getSummary() != null) {
            knowledge.setSummary(knowledgeData.getSummary());
        }
        if (knowledgeData.getCategory() != null) {
            knowledge.setCategory(knowledgeData.getCategory());
        }
        if (knowledgeData.getTags() != null) {
            knowledge.setTags(knowledgeData.getTags());
        }
        if (knowledgeData.getKeywords() != null) {
            knowledge.setKeywords(knowledgeData.getKeywords());
        }
        
        log.info("Updating knowledge: {}", id);
        return knowledgeRepository.save(knowledge);
    }
    
    /**
     * 删除知识
     */
    @Transactional
    public void deleteKnowledge(UUID id) {
        log.info("Deleting knowledge: {}", id);
        knowledgeRepository.deleteById(id);
    }
    
    /**
     * 获取精选知识
     */
    public List<Knowledge> getFeaturedKnowledge(int limit) {
        return knowledgeRepository.findByIsFeaturedTrueOrderByViewCountDesc(
            PageRequest.of(0, limit)
        );
    }
    
    /**
     * 获取各分类知识数量
     */
    public Map<String, Long> getCategoryStats() {
        List<Object[]> results = knowledgeRepository.countByCategory();
        return results.stream()
            .collect(Collectors.toMap(
                r -> ((Knowledge.KnowledgeCategory) r[0]).name(),
                r -> (Long) r[1]
            ));
    }
    
    /**
     * 记录知识使用
     */
    @Transactional
    public void recordUsage(UUID id) {
        knowledgeRepository.incrementUseCount(id);
    }
}
