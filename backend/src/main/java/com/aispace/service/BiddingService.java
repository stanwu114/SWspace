package com.aispace.service;

import com.aispace.dto.response.PageResponse;
import com.aispace.entity.BiddingItem;
import com.aispace.entity.BiddingSource;
import com.aispace.repository.BiddingItemRepository;
import com.aispace.repository.BiddingSourceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

/**
 * 招标监控服务
 * 负责招标信息的采集、分析和匹配
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BiddingService {
    
    private final BiddingSourceRepository sourceRepository;
    private final BiddingItemRepository itemRepository;
    
    // 用户关注的关键词配置（后续可从数据库/配置读取）
    private List<String> userKeywords = Arrays.asList(
        "智慧城市", "数字政府", "政务云", "大数据", "数据中台",
        "智慧交通", "智慧园区", "智慧社区", "城市大脑"
    );
    
    // ========== 数据源管理 ==========
    
    public List<BiddingSource> getAllSources() {
        return sourceRepository.findAll();
    }
    
    public List<BiddingSource> getActiveSources() {
        return sourceRepository.findByIsActiveTrue();
    }
    
    public Optional<BiddingSource> getSource(UUID id) {
        return sourceRepository.findById(id);
    }
    
    @Transactional
    public BiddingSource createSource(BiddingSource source) {
        log.info("Creating bidding source: {}", source.getName());
        return sourceRepository.save(source);
    }
    
    @Transactional
    public BiddingSource updateSource(UUID id, BiddingSource updates) {
        BiddingSource source = sourceRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Source not found: " + id));
        
        if (updates.getName() != null) source.setName(updates.getName());
        if (updates.getUrl() != null) source.setUrl(updates.getUrl());
        if (updates.getType() != null) source.setType(updates.getType());
        if (updates.getRegion() != null) source.setRegion(updates.getRegion());
        if (updates.getIsActive() != null) source.setIsActive(updates.getIsActive());
        if (updates.getCrawlConfig() != null) source.setCrawlConfig(updates.getCrawlConfig());
        if (updates.getCrawlInterval() != null) source.setCrawlInterval(updates.getCrawlInterval());
        if (updates.getFilterKeywords() != null) source.setFilterKeywords(updates.getFilterKeywords());
        
        return sourceRepository.save(source);
    }
    
    @Transactional
    public void deleteSource(UUID id) {
        sourceRepository.deleteById(id);
        log.info("Deleted bidding source: {}", id);
    }
    
    // ========== 招标信息管理 ==========
    
    public PageResponse<BiddingItem> listItems(
            String region, String industry, String keyword,
            Boolean matched, Boolean unread, Boolean starred,
            int page, int pageSize) {
        
        Pageable pageable = PageRequest.of(page - 1, pageSize, Sort.by(Sort.Direction.DESC, "publishDate"));
        Page<BiddingItem> result;
        
        if (starred != null && starred) {
            result = itemRepository.findByIsStarredTrue(pageable);
        } else if (matched != null && matched) {
            result = itemRepository.findByIsMatchedTrue(pageable);
        } else if (unread != null && unread) {
            result = itemRepository.findByIsReadFalse(pageable);
        } else {
            result = itemRepository.search(region, industry, keyword, pageable);
        }
        
        return PageResponse.<BiddingItem>builder()
            .items(result.getContent())
            .total(result.getTotalElements())
            .page(page)
            .pageSize(pageSize)
            .totalPages(result.getTotalPages())
            .build();
    }
    
    public Optional<BiddingItem> getItem(UUID id) {
        return itemRepository.findById(id);
    }
    
    @Transactional
    public BiddingItem createItem(BiddingItem item) {
        // 检查是否重复
        if (item.getExternalId() != null && item.getSourceId() != null) {
            if (itemRepository.existsByExternalIdAndSourceId(item.getExternalId(), item.getSourceId())) {
                log.debug("Bidding item already exists: {}", item.getExternalId());
                return itemRepository.findByExternalIdAndSourceId(item.getExternalId(), item.getSourceId())
                    .orElse(item);
            }
        }
        
        // 执行关键词匹配
        matchKeywords(item);
        
        log.info("Creating bidding item: {}", item.getTitle());
        return itemRepository.save(item);
    }
    
    @Transactional
    public void markAsRead(UUID id) {
        itemRepository.markAsRead(id);
    }
    
    @Transactional
    public void toggleStar(UUID id) {
        BiddingItem item = itemRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Item not found: " + id));
        item.setIsStarred(!item.getIsStarred());
        itemRepository.save(item);
    }
    
    @Transactional
    public void linkToProject(UUID itemId, UUID projectId) {
        BiddingItem item = itemRepository.findById(itemId)
            .orElseThrow(() -> new RuntimeException("Item not found: " + itemId));
        item.setProjectId(projectId);
        itemRepository.save(item);
        log.info("Linked bidding item {} to project {}", itemId, projectId);
    }
    
    // ========== 匹配与分析 ==========
    
    private void matchKeywords(BiddingItem item) {
        String searchText = (item.getTitle() + " " + 
                            (item.getContent() != null ? item.getContent() : "") + " " +
                            (item.getProjectName() != null ? item.getProjectName() : "")).toLowerCase();
        
        List<String> matchedKeywords = new ArrayList<>();
        for (String keyword : userKeywords) {
            if (searchText.contains(keyword.toLowerCase())) {
                matchedKeywords.add(keyword);
            }
        }
        
        if (!matchedKeywords.isEmpty()) {
            item.setIsMatched(true);
            item.setMatchScore((double) matchedKeywords.size() / userKeywords.size());
            item.setMatchReason("匹配关键词: " + String.join(", ", matchedKeywords));
            item.setKeywords(matchedKeywords);
            log.info("Matched bidding item: {} with keywords: {}", item.getTitle(), matchedKeywords);
        }
    }
    
    public List<BiddingItem> getMatchedItems() {
        return itemRepository.findMatchedItemsNotNotified();
    }
    
    public List<BiddingItem> getUpcomingDeadlines(int days) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime end = now.plusDays(days);
        return itemRepository.findByDeadlineAfterAndDeadlineBeforeOrderByDeadlineAsc(now, end);
    }
    
    // ========== 统计信息 ==========
    
    public Map<String, Object> getStatistics() {
        Map<String, Object> stats = new HashMap<>();
        
        // 今日新增
        LocalDateTime todayStart = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0);
        long todayCount = itemRepository.count(); // 简化实现
        
        // 匹配数量
        long matchedCount = itemRepository.findByIsMatchedTrue(PageRequest.of(0, 1)).getTotalElements();
        
        // 未读数量
        long unreadCount = itemRepository.findByIsReadFalse(PageRequest.of(0, 1)).getTotalElements();
        
        // 即将截止
        List<BiddingItem> upcoming = getUpcomingDeadlines(7);
        
        stats.put("todayCount", todayCount);
        stats.put("matchedCount", matchedCount);
        stats.put("unreadCount", unreadCount);
        stats.put("upcomingDeadlines", upcoming.size());
        stats.put("activeSources", sourceRepository.findByIsActiveTrue().size());
        
        return stats;
    }
    
    public List<String> getAvailableRegions() {
        return itemRepository.findDistinctRegions();
    }
    
    public List<String> getAvailableIndustries() {
        return itemRepository.findDistinctIndustries();
    }
    
    // ========== 定时任务 ==========
    
    /**
     * 定时检查需要发送通知的招标信息
     * 每30分钟执行一次
     */
    @Scheduled(fixedRate = 1800000)
    @Transactional
    public void checkAndNotify() {
        List<BiddingItem> toNotify = itemRepository.findMatchedItemsNotNotified();
        if (!toNotify.isEmpty()) {
            log.info("Found {} matched items to notify", toNotify.size());
            // 通知逻辑将在NotificationService中实现
            for (BiddingItem item : toNotify) {
                itemRepository.markNotificationSent(item.getId());
            }
        }
    }
    
    /**
     * 更新用户关注的关键词
     */
    public void updateUserKeywords(List<String> keywords) {
        this.userKeywords = new ArrayList<>(keywords);
        log.info("Updated user keywords: {}", keywords);
    }
    
    public List<String> getUserKeywords() {
        return new ArrayList<>(userKeywords);
    }
    
    // ========== 模拟数据采集（实际项目中应替换为真实爬虫） ==========
    
    /**
     * 模拟从数据源采集数据
     * 实际项目中应实现真实的网页爬虫或API调用
     */
    @Transactional
    public int crawlSource(UUID sourceId) {
        BiddingSource source = sourceRepository.findById(sourceId)
            .orElseThrow(() -> new RuntimeException("Source not found: " + sourceId));
        
        log.info("Starting crawl for source: {}", source.getName());
        
        // 更新最后采集时间
        source.setLastCrawlAt(LocalDateTime.now());
        sourceRepository.save(source);
        
        // 这里应该是实际的爬虫逻辑
        // 目前返回模拟结果
        log.info("Crawl completed for source: {} (simulated)", source.getName());
        return 0;
    }
    
    /**
     * 初始化默认数据源
     */
    @Transactional
    public void initDefaultSources() {
        if (sourceRepository.count() == 0) {
            log.info("Initializing default bidding sources...");
            
            // 全国公共资源交易平台
            createSource(BiddingSource.builder()
                .name("中国政府采购网")
                .url("http://www.ccgp.gov.cn")
                .type(BiddingSource.SourceType.NATIONAL)
                .region("全国")
                .isActive(true)
                .crawlInterval(3600)
                .filterKeywords(userKeywords)
                .build());
            
            // 全国公共资源交易平台
            createSource(BiddingSource.builder()
                .name("全国公共资源交易平台")
                .url("http://www.ggzy.gov.cn")
                .type(BiddingSource.SourceType.NATIONAL)
                .region("全国")
                .isActive(true)
                .crawlInterval(3600)
                .filterKeywords(userKeywords)
                .build());
            
            log.info("Default bidding sources initialized");
        }
    }
}
