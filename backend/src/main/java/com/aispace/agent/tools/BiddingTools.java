package com.aispace.agent.tools;

import com.aispace.entity.BiddingItem;
import com.aispace.repository.BiddingItemRepository;
import com.aispace.service.BiddingService;
import io.agentscope.core.tool.Tool;
import io.agentscope.core.tool.ToolParam;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * 招投标管理工具集 - 使用 AgentScope 官方 @Tool 注解
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BiddingTools {

    private final BiddingService biddingService;
    private final BiddingItemRepository biddingItemRepository;

    /**
     * 查询招标信息列表
     */
    @Tool(description = "获取招标信息列表，支持按地区、行业、关键词筛选")
    public String listBiddingItems(
            @ToolParam(name = "keyword", description = "关键词搜索，可为空") String keyword,
            @ToolParam(name = "region", description = "地区筛选，可为空") String region) {
        try {
            List<BiddingItem> items = biddingItemRepository.search(
                region, null, keyword,
                PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "publishDate"))
            ).getContent();
            
            if (items.isEmpty()) {
                return "未找到招标信息";
            }

            StringBuilder sb = new StringBuilder("招标信息列表：\n");
            for (BiddingItem item : items) {
                sb.append(String.format("- ID: %s, 标题: %s, 地区: %s, 截止日期: %s\n",
                    item.getId(),
                    item.getTitle(),
                    item.getRegion(),
                    formatDateTime(item.getDeadline())));
            }
            return sb.toString();
        } catch (Exception e) {
            log.error("查询招标列表失败", e);
            return "查询失败: " + e.getMessage();
        }
    }

    /**
     * 查询招标详情
     */
    @Tool(description = "获取招标信息详情")
    public String getBiddingDetail(
            @ToolParam(name = "biddingId", description = "招标信息ID (UUID格式)") String biddingId) {
        try {
            UUID id = UUID.fromString(biddingId);
            Optional<BiddingItem> optItem = biddingService.getItem(id);
            if (optItem.isEmpty()) {
                return "未找到招标信息 ID: " + biddingId;
            }
            BiddingItem item = optItem.get();
            return String.format("""
                招标详情：
                - ID: %s
                - 标题: %s
                - 项目名称: %s
                - 采购人: %s
                - 代理机构: %s
                - 类型: %s
                - 预算: %s
                - 发布日期: %s
                - 截止日期: %s
                - 地区: %s
                - 行业: %s
                - 匹配: %s
                - 内容摘要:
                %s
                """,
                item.getId(),
                item.getTitle(),
                item.getProjectName(),
                item.getPurchaser(),
                item.getAgency(),
                item.getBidType(),
                item.getBudget() != null ? item.getBudget() : "未标注",
                formatDate(item.getPublishDate()),
                formatDateTime(item.getDeadline()),
                item.getRegion(),
                item.getIndustry(),
                item.getIsMatched() ? "是 (原因: " + item.getMatchReason() + ")" : "否",
                item.getSummary() != null ? item.getSummary() 
                    : (item.getContent() != null && item.getContent().length() > 300
                        ? item.getContent().substring(0, 300) + "..."
                        : item.getContent())
            );
        } catch (IllegalArgumentException e) {
            return "无效的招标ID格式: " + biddingId;
        } catch (Exception e) {
            log.error("查询招标详情失败", e);
            return "查询失败: " + e.getMessage();
        }
    }

    /**
     * 添加招标信息
     */
    @Tool(description = "添加新的招标信息")
    public String addBiddingItem(
            @ToolParam(name = "title", description = "标题") String title,
            @ToolParam(name = "content", description = "内容") String content,
            @ToolParam(name = "sourceId", description = "数据源ID (UUID格式)") String sourceId,
            @ToolParam(name = "deadline", description = "截止日期，格式：yyyy-MM-dd") String deadline) {
        try {
            BiddingItem item = new BiddingItem();
            item.setTitle(title);
            item.setContent(content);
            if (sourceId != null && !sourceId.isEmpty()) {
                item.setSourceId(UUID.fromString(sourceId));
            }
            
            if (deadline != null && !deadline.isEmpty()) {
                item.setDeadline(LocalDateTime.parse(deadline + "T23:59:59"));
            }
            
            BiddingItem saved = biddingService.createItem(item);
            return "招标信息添加成功，ID: " + saved.getId();
        } catch (IllegalArgumentException e) {
            return "参数格式错误: " + e.getMessage();
        } catch (Exception e) {
            log.error("添加招标信息失败", e);
            return "添加失败: " + e.getMessage();
        }
    }

    /**
     * 标记招标信息为已读
     */
    @Tool(description = "标记招标信息为已读")
    public String markBiddingAsRead(
            @ToolParam(name = "biddingId", description = "招标信息ID (UUID格式)") String biddingId) {
        try {
            UUID id = UUID.fromString(biddingId);
            biddingService.markAsRead(id);
            return "已标记为已读";
        } catch (IllegalArgumentException e) {
            return "无效的招标ID格式: " + biddingId;
        } catch (Exception e) {
            log.error("标记已读失败", e);
            return "操作失败: " + e.getMessage();
        }
    }

    /**
     * 获取匹配的招标信息
     */
    @Tool(description = "获取与用户关注关键词匹配的招标信息")
    public String getMatchedBiddings() {
        try {
            List<BiddingItem> items = biddingService.getMatchedItems();
            if (items.isEmpty()) {
                return "暂无匹配的招标信息";
            }
            StringBuilder sb = new StringBuilder("匹配的招标信息：\n");
            for (BiddingItem item : items) {
                sb.append(String.format("- ID: %s, 标题: %s, 匹配原因: %s, 截止: %s\n",
                    item.getId(), item.getTitle(), item.getMatchReason(),
                    formatDateTime(item.getDeadline())));
            }
            return sb.toString();
        } catch (Exception e) {
            log.error("查询匹配招标失败", e);
            return "查询失败: " + e.getMessage();
        }
    }

    /**
     * 获取即将截止的招标
     */
    @Tool(description = "获取即将截止的招标信息，默认7天内")
    public String getUpcomingDeadlines(
            @ToolParam(name = "days", description = "天数范围，默认7天") Integer days) {
        try {
            int d = days != null ? days : 7;
            List<BiddingItem> items = biddingService.getUpcomingDeadlines(d);
            if (items.isEmpty()) {
                return "未来" + d + "天内没有即将截止的招标信息";
            }
            StringBuilder sb = new StringBuilder("即将截止的招标信息（" + d + "天内）：\n");
            for (BiddingItem item : items) {
                sb.append(String.format("- ID: %s, 标题: %s, 截止: %s\n",
                    item.getId(), item.getTitle(), formatDateTime(item.getDeadline())));
            }
            return sb.toString();
        } catch (Exception e) {
            log.error("查询即将截止招标失败", e);
            return "查询失败: " + e.getMessage();
        }
    }

    private String formatDateTime(LocalDateTime date) {
        if (date == null) return "未设置";
        return date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
    }

    private String formatDate(LocalDate date) {
        if (date == null) return "未设置";
        return date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
    }
}
