package com.aispace.service;

import com.aispace.dto.response.PageResponse;
import com.aispace.entity.Interaction;
import com.aispace.repository.InteractionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 交互记录服务层
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class InteractionService {
    
    private final InteractionRepository interactionRepository;
    
    /**
     * 获取客户的交互记录（分页）
     */
    public PageResponse<Interaction> listByCustomer(UUID customerId, int page, int pageSize) {
        Pageable pageable = PageRequest.of(page - 1, pageSize);
        Page<Interaction> result = interactionRepository.findByCustomerIdOrderByInteractionAtDesc(
            customerId, pageable
        );
        
        return PageResponse.of(
            result.getContent(),
            result.getTotalElements(),
            page,
            pageSize
        );
    }
    
    /**
     * 获取项目的交互记录
     */
    public PageResponse<Interaction> listByProject(UUID projectId, int page, int pageSize) {
        Pageable pageable = PageRequest.of(page - 1, pageSize);
        Page<Interaction> result = interactionRepository.findByProjectIdOrderByInteractionAtDesc(
            projectId, pageable
        );
        
        return PageResponse.of(
            result.getContent(),
            result.getTotalElements(),
            page,
            pageSize
        );
    }
    
    /**
     * 按类型筛选交互记录
     */
    public PageResponse<Interaction> listByType(
        UUID customerId, 
        Interaction.InteractionType type, 
        int page, 
        int pageSize
    ) {
        Pageable pageable = PageRequest.of(page - 1, pageSize);
        Page<Interaction> result = interactionRepository.findByCustomerIdAndTypeOrderByInteractionAtDesc(
            customerId, type, pageable
        );
        
        return PageResponse.of(
            result.getContent(),
            result.getTotalElements(),
            page,
            pageSize
        );
    }
    
    /**
     * 获取交互详情
     */
    public Optional<Interaction> getInteraction(UUID id) {
        return interactionRepository.findById(id);
    }
    
    /**
     * 创建交互记录
     */
    @Transactional
    public Interaction createInteraction(Interaction interaction) {
        log.info("Creating interaction for customer: {}, type: {}", 
            interaction.getCustomerId(), interaction.getType());
        return interactionRepository.save(interaction);
    }
    
    /**
     * 更新交互记录
     */
    @Transactional
    public Interaction updateInteraction(UUID id, Interaction interactionData) {
        Interaction interaction = interactionRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Interaction not found: " + id));
        
        if (interactionData.getType() != null) {
            interaction.setType(interactionData.getType());
        }
        if (interactionData.getSubject() != null) {
            interaction.setSubject(interactionData.getSubject());
        }
        if (interactionData.getContent() != null) {
            interaction.setContent(interactionData.getContent());
        }
        if (interactionData.getSummary() != null) {
            interaction.setSummary(interactionData.getSummary());
        }
        if (interactionData.getKeyPoints() != null) {
            interaction.setKeyPoints(interactionData.getKeyPoints());
        }
        if (interactionData.getSentiment() != null) {
            interaction.setSentiment(interactionData.getSentiment());
        }
        if (interactionData.getNextActions() != null) {
            interaction.setNextActions(interactionData.getNextActions());
        }
        if (interactionData.getNextActionAt() != null) {
            interaction.setNextActionAt(interactionData.getNextActionAt());
        }
        if (interactionData.getDuration() != null) {
            interaction.setDuration(interactionData.getDuration());
        }
        if (interactionData.getLocation() != null) {
            interaction.setLocation(interactionData.getLocation());
        }
        
        log.info("Updating interaction: {}", id);
        return interactionRepository.save(interaction);
    }
    
    /**
     * 删除交互记录
     */
    @Transactional
    public void deleteInteraction(UUID id) {
        log.info("Deleting interaction: {}", id);
        interactionRepository.deleteById(id);
    }
    
    /**
     * 获取最近的交互
     */
    public List<Interaction> getRecentInteractions(UUID customerId) {
        return interactionRepository.findTop5ByCustomerIdOrderByInteractionAtDesc(customerId);
    }
    
    /**
     * 获取待跟进事项
     */
    public List<Interaction> getPendingFollowUps(LocalDateTime deadline) {
        return interactionRepository.findPendingFollowUps(deadline);
    }
    
    /**
     * 获取时间范围内的交互
     */
    public List<Interaction> getByTimeRange(UUID customerId, LocalDateTime start, LocalDateTime end) {
        return interactionRepository.findByCustomerIdAndTimeRange(customerId, start, end);
    }
    
    /**
     * 获取交互类型统计
     */
    public Map<String, Long> getTypeStats(UUID customerId) {
        List<Object[]> results = interactionRepository.countByCustomerIdGroupByType(customerId);
        return results.stream()
            .collect(Collectors.toMap(
                r -> ((Interaction.InteractionType) r[0]).name(),
                r -> (Long) r[1]
            ));
    }
    
    /**
     * 获取情感统计
     */
    public Map<String, Long> getSentimentStats(UUID customerId) {
        List<Object[]> results = interactionRepository.countByCustomerIdGroupBySentiment(customerId);
        return results.stream()
            .collect(Collectors.toMap(
                r -> ((Interaction.Sentiment) r[0]).name(),
                r -> (Long) r[1]
            ));
    }
    
    /**
     * 统计交互次数
     */
    public long countByCustomer(UUID customerId) {
        return interactionRepository.countByCustomerId(customerId);
    }
}
