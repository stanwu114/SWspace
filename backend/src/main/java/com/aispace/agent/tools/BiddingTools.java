package com.aispace.agent.tools;

import com.aispace.dto.response.PageResponse;
import com.aispace.entity.BiddingItem;
import com.aispace.service.BiddingService;
import io.agentscope.core.tool.Tool;
import io.agentscope.core.tool.ToolParam;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * 招投标管理工具集 - 使用 AgentScope 官方 @Tool 注解
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BiddingTools {

    private final BiddingService biddingService;

    /**
     * 查询招标信息列表
     */
    @Tool(description = "获取招标信息列表，支持按地区、行业和关键词筛选")
    public String listBiddingItems(
            @ToolParam(name = "region", description = "地区筛选") String region,
            @ToolParam(name = "industry", description = "行业筛选") String industry,
            @ToolParam(name = "keyword", description = "关键词搜索") String keyword,
            @ToolParam(name = "matched", description = "是否只显示匹配的招标") Boolean matched) {
        try {
            PageResponse<BiddingItem> result = biddingService.listItems(
                region, industry, keyword,
                matched, false, false,
                1, 20
            );
            
            if (result.getItems().isEmpty()) {
                return "未找到招标信息";
            }

            StringBuilder sb = new StringBuilder("招标信息列表：\n");
            for (BiddingItem item : result.getItems()) {
                sb.append(String.format("- ID: %s, 标题: %s, 地区: %s, 预算: %s, 截止: %s\n",
                    item.getId(), 
                    truncate(item.getTitle(), 50), 
                    item.getRegion(),
                    item.getBudget() != null ? item.getBudget().toString() : "未标注",
                    formatDate(item.getDeadline())));
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
            @ToolParam(name = "biddingId", description = "招标信息ID") String biddingId) {
        try {
            return biddingService.getItem(UUID.fromString(biddingId))
                .map(item -> String.format(
                    "招标详情：\n- ID: %s\n- 标题: %s\n- 采购人: %s\n- 代理机构: %s\n- 预算: %s\n- 地区: %s\n- 行业: %s\n- 发布日期: %s\n- 截止日期: %s\n- 招标类型: %s\n- 内容摘要:\n%s\n- 匹配得分: %s\n- 匹配原因: %s\n",
                    item.getId(),
                    item.getTitle(),
                    item.getPurchaser(),
                    item.getAgency(),
                    item.getBudget() != null ? item.getBudget().toString() : "未标注",
                    item.getRegion(),
                    item.getIndustry(),
                    item.getPublishDate(),
                    formatDate(item.getDeadline()),
                    item.getBidType(),
                    truncate(item.getContent(), 300),
                    item.getMatchScore() != null ? String.format("%.0f%%", item.getMatchScore() * 100) : "未匹配",
                    item.getMatchReason() != null ? item.getMatchReason() : "无"
                ))
                .orElse("未找到招标信息 ID: " + biddingId);
        } catch (Exception e) {
            log.error("查询招标详情失败", e);
            return "查询失败: " + e.getMessage();
        }
    }

    /**
     * 获取匹配的招标
     */
    @Tool(description = "获取与公司业务匹配的招标信息")
    public String getMatchedItems() {
        try {
            var items = biddingService.getMatchedItems();
            if (items.isEmpty()) {
                return "暂无匹配的招标信息";
            }
            
            StringBuilder sb = new StringBuilder("匹配的招标信息：\n");
            for (BiddingItem item : items) {
                sb.append(String.format("- %s (ID: %s)\n  截止: %s, 匹配度: %.0f%%\n",
                    truncate(item.getTitle(), 40), 
                    item.getId(),
                    formatDate(item.getDeadline()),
                    item.getMatchScore() != null ? item.getMatchScore() * 100 : 0));
            }
            return sb.toString();
        } catch (Exception e) {
            log.error("获取匹配招标失败", e);
            return "查询失败: " + e.getMessage();
        }
    }

    /**
     * 获取即将到期的招标
     */
    @Tool(description = "获取即将到截止日期的招标信息")
    public String getUpcomingDeadlines(
            @ToolParam(name = "days", description = "查询未来多少天内到期的招标") Integer days) {
        try {
            var items = biddingService.getUpcomingDeadlines(days != null ? days : 7);
            if (items.isEmpty()) {
                return "未来 " + (days != null ? days : 7) + " 天内无即将到期的招标";
            }
            
            StringBuilder sb = new StringBuilder("即将到期的招标：\n");
            for (BiddingItem item : items) {
                sb.append(String.format("- %s\n  截止: %s, 预算: %s\n",
                    truncate(item.getTitle(), 40),
                    formatDate(item.getDeadline()),
                    item.getBudget() != null ? item.getBudget().toString() : "未标注"));
            }
            return sb.toString();
        } catch (Exception e) {
            log.error("获取即将到期招标失败", e);
            return "查询失败: " + e.getMessage();
        }
    }

    /**
     * 收藏/取消收藏招标
     */
    @Tool(description = "收藏或取消收藏招标信息")
    public String toggleStar(
            @ToolParam(name = "biddingId", description = "招标信息ID") String biddingId) {
        try {
            biddingService.toggleStar(UUID.fromString(biddingId));
            return "收藏状态已切换";
        } catch (Exception e) {
            log.error("切换收藏状态失败", e);
            return "操作失败: " + e.getMessage();
        }
    }

    /**
     * 标记已读
     */
    @Tool(description = "将招标信息标记为已读")
    public String markAsRead(
            @ToolParam(name = "biddingId", description = "招标信息ID") String biddingId) {
        try {
            biddingService.markAsRead(UUID.fromString(biddingId));
            return "已标记为已读";
        } catch (Exception e) {
            log.error("标记已读失败", e);
            return "操作失败: " + e.getMessage();
        }
    }

    /**
     * 关联项目
     */
    @Tool(description = "将招标信息关联到项目")
    public String linkToProject(
            @ToolParam(name = "biddingId", description = "招标信息ID") String biddingId,
            @ToolParam(name = "projectId", description = "项目ID") String projectId) {
        try {
            biddingService.linkToProject(UUID.fromString(biddingId), UUID.fromString(projectId));
            return "已将招标信息关联到项目";
        } catch (Exception e) {
            log.error("关联项目失败", e);
            return "操作失败: " + e.getMessage();
        }
    }

    /**
     * 获取统计信息
     */
    @Tool(description = "获取招标统计信息")
    public String getStatistics() {
        try {
            var stats = biddingService.getStatistics();
            return String.format(
                "招标统计：\n- 今日新增: %d\n- 匹配数量: %d\n- 未读数量: %d\n- 即将截止: %d\n- 活跃数据源: %d\n",
                stats.get("todayCount"),
                stats.get("matchedCount"),
                stats.get("unreadCount"),
                stats.get("upcomingDeadlines"),
                stats.get("activeSources")
            );
        } catch (Exception e) {
            log.error("获取统计信息失败", e);
            return "查询失败: " + e.getMessage();
        }
    }

    private String formatDate(LocalDateTime date) {
        if (date == null) return "未设置";
        return date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
    }

    private String truncate(String text, int maxLength) {
        if (text == null) return "";
        if (text.length() <= maxLength) return text;
        return text.substring(0, maxLength) + "...";
    }
}
