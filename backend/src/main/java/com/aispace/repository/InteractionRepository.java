package com.aispace.repository;

import com.aispace.entity.Interaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * 交互记录数据访问层
 */
@Repository
public interface InteractionRepository extends JpaRepository<Interaction, UUID> {
    
    /**
     * 根据客户ID查询交互记录
     */
    Page<Interaction> findByCustomerIdOrderByInteractionAtDesc(UUID customerId, Pageable pageable);
    
    /**
     * 根据项目ID查询
     */
    Page<Interaction> findByProjectIdOrderByInteractionAtDesc(UUID projectId, Pageable pageable);
    
    /**
     * 根据类型查询
     */
    Page<Interaction> findByCustomerIdAndTypeOrderByInteractionAtDesc(
        UUID customerId, Interaction.InteractionType type, Pageable pageable
    );
    
    /**
     * 查询时间范围内的交互
     */
    @Query("SELECT i FROM Interaction i WHERE i.customerId = :customerId " +
           "AND i.interactionAt BETWEEN :startTime AND :endTime " +
           "ORDER BY i.interactionAt DESC")
    List<Interaction> findByCustomerIdAndTimeRange(
        @Param("customerId") UUID customerId,
        @Param("startTime") LocalDateTime startTime,
        @Param("endTime") LocalDateTime endTime
    );
    
    /**
     * 查询最近的交互记录
     */
    List<Interaction> findTop5ByCustomerIdOrderByInteractionAtDesc(UUID customerId);
    
    /**
     * 查询需要跟进的交互
     */
    @Query("SELECT i FROM Interaction i WHERE i.nextActionAt IS NOT NULL " +
           "AND i.nextActionAt <= :deadline ORDER BY i.nextActionAt ASC")
    List<Interaction> findPendingFollowUps(@Param("deadline") LocalDateTime deadline);
    
    /**
     * 统计客户交互次数
     */
    long countByCustomerId(UUID customerId);
    
    /**
     * 按类型统计
     */
    @Query("SELECT i.type, COUNT(i) FROM Interaction i WHERE i.customerId = :customerId GROUP BY i.type")
    List<Object[]> countByCustomerIdGroupByType(@Param("customerId") UUID customerId);
    
    /**
     * 按情感统计
     */
    @Query("SELECT i.sentiment, COUNT(i) FROM Interaction i WHERE i.customerId = :customerId " +
           "AND i.sentiment IS NOT NULL GROUP BY i.sentiment")
    List<Object[]> countByCustomerIdGroupBySentiment(@Param("customerId") UUID customerId);
}
