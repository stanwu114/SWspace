package com.aispace.repository;

import com.aispace.entity.Case;
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
 * 案例Repository
 */
@Repository
public interface CaseRepository extends JpaRepository<Case, UUID> {
    
    Page<Case> findByStatus(Case.CaseStatus status, Pageable pageable);
    
    Page<Case> findByIndustry(String industry, Pageable pageable);
    
    Page<Case> findByRegion(String region, Pageable pageable);
    
    Page<Case> findByCustomerType(Case.CustomerType customerType, Pageable pageable);
    
    @Query("SELECT c FROM Case c WHERE c.status = 'PUBLISHED' AND c.isFeatured = true " +
           "ORDER BY c.viewCount DESC")
    List<Case> findFeaturedCases(Pageable pageable);
    
    @Query("SELECT c FROM Case c WHERE c.status = 'PUBLISHED' " +
           "ORDER BY c.createdAt DESC")
    Page<Case> findPublishedCases(Pageable pageable);
    
    @Query("SELECT c FROM Case c WHERE " +
           "(:industry IS NULL OR c.industry = :industry) AND " +
           "(:region IS NULL OR c.region = :region) AND " +
           "(:customerType IS NULL OR c.customerType = :customerType) AND " +
           "c.status = 'PUBLISHED'")
    Page<Case> searchCases(
            @Param("industry") String industry,
            @Param("region") String region,
            @Param("customerType") Case.CustomerType customerType,
            Pageable pageable);
    
    @Query("SELECT c FROM Case c WHERE c.status = 'PUBLISHED' AND " +
           "(LOWER(c.title) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(c.description) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(c.solution) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    Page<Case> searchByKeyword(@Param("keyword") String keyword, Pageable pageable);
    
    @Modifying
    @Query("UPDATE Case c SET c.viewCount = c.viewCount + 1 WHERE c.id = :id")
    void incrementViewCount(@Param("id") UUID id);
    
    @Modifying
    @Query("UPDATE Case c SET c.referenceCount = c.referenceCount + 1 WHERE c.id = :id")
    void incrementReferenceCount(@Param("id") UUID id);
    
    @Query("SELECT DISTINCT c.industry FROM Case c WHERE c.industry IS NOT NULL ORDER BY c.industry")
    List<String> findDistinctIndustries();
    
    @Query("SELECT DISTINCT c.region FROM Case c WHERE c.region IS NOT NULL ORDER BY c.region")
    List<String> findDistinctRegions();
    
    @Query("SELECT c.industry, COUNT(c) FROM Case c WHERE c.status = 'PUBLISHED' " +
           "GROUP BY c.industry ORDER BY COUNT(c) DESC")
    List<Object[]> countByIndustry();
    
    List<Case> findByProjectId(UUID projectId);
}
