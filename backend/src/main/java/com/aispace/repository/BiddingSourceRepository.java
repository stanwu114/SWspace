package com.aispace.repository;

import com.aispace.entity.BiddingSource;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * 招标数据源Repository
 */
@Repository
public interface BiddingSourceRepository extends JpaRepository<BiddingSource, UUID> {
    
    List<BiddingSource> findByIsActiveTrue();
    
    List<BiddingSource> findByType(BiddingSource.SourceType type);
    
    List<BiddingSource> findByRegion(String region);
    
    @Query("SELECT s FROM BiddingSource s WHERE s.isActive = true AND " +
           "(s.lastCrawlAt IS NULL OR s.lastCrawlAt < :threshold)")
    List<BiddingSource> findSourcesNeedingCrawl(LocalDateTime threshold);
    
    @Query("SELECT s FROM BiddingSource s WHERE s.isActive = true ORDER BY s.lastCrawlAt ASC NULLS FIRST")
    List<BiddingSource> findActiveSourcesOrderByLastCrawl();
    
    boolean existsByUrlAndIsActiveTrue(String url);
}
