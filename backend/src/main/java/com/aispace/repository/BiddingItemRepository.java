package com.aispace.repository;

import com.aispace.entity.BiddingItem;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * 招标信息Repository
 */
@Repository
public interface BiddingItemRepository extends JpaRepository<BiddingItem, UUID> {
    
    Page<BiddingItem> findBySourceId(UUID sourceId, Pageable pageable);
    
    Page<BiddingItem> findByIsMatchedTrue(Pageable pageable);
    
    Page<BiddingItem> findByIsReadFalse(Pageable pageable);
    
    Page<BiddingItem> findByIsStarredTrue(Pageable pageable);
    
    List<BiddingItem> findByDeadlineAfterAndDeadlineBeforeOrderByDeadlineAsc(
            LocalDateTime start, LocalDateTime end);
    
    @Query("SELECT b FROM BiddingItem b WHERE b.isMatched = true AND b.notificationSent = false")
    List<BiddingItem> findMatchedItemsNotNotified();
    
    @Query("SELECT b FROM BiddingItem b WHERE " +
           "(:region IS NULL OR b.region = :region) AND " +
           "(:industry IS NULL OR b.industry = :industry) AND " +
           "(:keyword IS NULL OR LOWER(b.title) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(b.content) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    Page<BiddingItem> search(
            @Param("region") String region,
            @Param("industry") String industry,
            @Param("keyword") String keyword,
            Pageable pageable);
    
    @Query("SELECT b FROM BiddingItem b WHERE b.deadline > :now AND b.isMatched = true " +
           "ORDER BY b.deadline ASC")
    List<BiddingItem> findUpcomingMatchedDeadlines(@Param("now") LocalDateTime now);
    
    @Query("SELECT COUNT(b) FROM BiddingItem b WHERE b.sourceId = :sourceId AND " +
           "b.createdAt > :since")
    long countRecentBySource(@Param("sourceId") UUID sourceId, @Param("since") LocalDateTime since);
    
    Optional<BiddingItem> findByExternalIdAndSourceId(String externalId, UUID sourceId);
    
    boolean existsByExternalIdAndSourceId(String externalId, UUID sourceId);
    
    @Modifying
    @Query("UPDATE BiddingItem b SET b.isRead = true WHERE b.id = :id")
    void markAsRead(@Param("id") UUID id);
    
    @Modifying
    @Query("UPDATE BiddingItem b SET b.notificationSent = true WHERE b.id = :id")
    void markNotificationSent(@Param("id") UUID id);
    
    @Query("SELECT DISTINCT b.region FROM BiddingItem b WHERE b.region IS NOT NULL ORDER BY b.region")
    List<String> findDistinctRegions();
    
    @Query("SELECT DISTINCT b.industry FROM BiddingItem b WHERE b.industry IS NOT NULL ORDER BY b.industry")
    List<String> findDistinctIndustries();
    
    @Query("SELECT b FROM BiddingItem b WHERE b.bidType = :bidType ORDER BY b.publishDate DESC")
    Page<BiddingItem> findByBidType(@Param("bidType") BiddingItem.BidType bidType, Pageable pageable);
}
