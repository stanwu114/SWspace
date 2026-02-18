package com.aispace.repository;

import com.aispace.entity.Knowledge;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * 知识库数据访问层
 */
@Repository
public interface KnowledgeRepository extends JpaRepository<Knowledge, UUID> {
    
    /**
     * 根据分类查询
     */
    Page<Knowledge> findByCategory(Knowledge.KnowledgeCategory category, Pageable pageable);
    
    /**
     * 根据子分类查询
     */
    Page<Knowledge> findByCategoryAndSubcategory(
        Knowledge.KnowledgeCategory category, 
        String subcategory, 
        Pageable pageable
    );
    
    /**
     * 查询精选知识
     */
    List<Knowledge> findByIsFeaturedTrueOrderByViewCountDesc(Pageable pageable);
    
    /**
     * 关键词搜索
     */
    @Query("SELECT k FROM Knowledge k WHERE " +
           "LOWER(k.title) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(k.content) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    Page<Knowledge> searchByKeyword(@Param("keyword") String keyword, Pageable pageable);
    
    /**
     * 增加查看次数
     */
    @Modifying
    @Query("UPDATE Knowledge k SET k.viewCount = k.viewCount + 1 WHERE k.id = :id")
    void incrementViewCount(@Param("id") UUID id);
    
    /**
     * 增加使用次数
     */
    @Modifying
    @Query("UPDATE Knowledge k SET k.useCount = k.useCount + 1 WHERE k.id = :id")
    void incrementUseCount(@Param("id") UUID id);
    
    /**
     * 统计各分类数量
     */
    @Query("SELECT k.category, COUNT(k) FROM Knowledge k GROUP BY k.category")
    List<Object[]> countByCategory();
}
