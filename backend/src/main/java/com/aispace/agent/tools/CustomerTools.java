package com.aispace.agent.tools;

import com.aispace.dto.response.PageResponse;
import com.aispace.entity.Customer;
import com.aispace.service.CustomerService;
import io.agentscope.core.tool.Tool;
import io.agentscope.core.tool.ToolParam;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * 客户管理工具集 - 使用 AgentScope 官方 @Tool 注解
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CustomerTools {

    private final CustomerService customerService;

    /**
     * 查询客户列表
     */
    @Tool(description = "获取客户列表，支持按类型、级别、地区和关键词搜索")
    public String listCustomers(
            @ToolParam(name = "type", description = "客户类型: ENTERPRISE, SMB, GOVERNMENT, INDIVIDUAL") String type,
            @ToolParam(name = "level", description = "客户级别: STRATEGIC, KEY, NORMAL") String level,
            @ToolParam(name = "region", description = "地区筛选") String region,
            @ToolParam(name = "keyword", description = "搜索关键词") String keyword) {
        try {
            Customer.CustomerType customerType = type != null && !type.isEmpty() 
                ? Customer.CustomerType.valueOf(type) : null;
            Customer.CustomerLevel customerLevel = level != null && !level.isEmpty()
                ? Customer.CustomerLevel.valueOf(level) : null;
            
            PageResponse<Customer> result = customerService.listCustomers(
                customerType, customerLevel, region, keyword,
                1, 20, "createdAt", "desc"
            );
            
            if (result.getItems().isEmpty()) {
                return "未找到客户记录";
            }

            StringBuilder sb = new StringBuilder("客户列表：\n");
            for (Customer c : result.getItems()) {
                sb.append(String.format("- ID: %s, 名称: %s, 行业: %s, 类型: %s, 级别: %s\n",
                    c.getId(), c.getName(), c.getIndustry(), c.getType(), c.getLevel()));
            }
            return sb.toString();
        } catch (Exception e) {
            log.error("查询客户列表失败", e);
            return "查询失败: " + e.getMessage();
        }
    }

    /**
     * 查询客户详情
     */
    @Tool(description = "获取指定客户的详细信息")
    public String getCustomerDetail(
            @ToolParam(name = "customerId", description = "客户ID") String customerId) {
        try {
            return customerService.getCustomer(UUID.fromString(customerId))
                .map(customer -> String.format("""
                    客户详情：
                    - ID: %s
                    - 名称: %s
                    - 简称: %s
                    - 类型: %s
                    - 行业: %s
                    - 地区: %s
                    - 级别: %s
                    - 地址: %s
                    - 网站: %s
                    - 关系评分: %d
                    - 创建时间: %s
                    """,
                    customer.getId(),
                    customer.getName(),
                    customer.getShortName(),
                    customer.getType(),
                    customer.getIndustry(),
                    customer.getRegion(),
                    customer.getLevel(),
                    customer.getAddress(),
                    customer.getWebsite(),
                    customer.getRelationshipScore(),
                    customer.getCreatedAt()
                ))
                .orElse("未找到客户 ID: " + customerId);
        } catch (Exception e) {
            log.error("查询客户详情失败", e);
            return "查询失败: " + e.getMessage();
        }
    }

    /**
     * 创建客户
     */
    @Tool(description = "创建新客户")
    public String createCustomer(
            @ToolParam(name = "name", description = "客户名称") String name,
            @ToolParam(name = "type", description = "客户类型: ENTERPRISE, SMB, GOVERNMENT, INDIVIDUAL") String type,
            @ToolParam(name = "industry", description = "所属行业") String industry,
            @ToolParam(name = "region", description = "所在地区") String region) {
        try {
            Customer customer = Customer.builder()
                .name(name)
                .type(type != null && !type.isEmpty() 
                    ? Customer.CustomerType.valueOf(type) 
                    : Customer.CustomerType.ENTERPRISE)
                .industry(industry)
                .region(region)
                .build();
            
            Customer saved = customerService.createCustomer(customer);
            return "客户创建成功，ID: " + saved.getId();
        } catch (Exception e) {
            log.error("创建客户失败", e);
            return "创建失败: " + e.getMessage();
        }
    }

    /**
     * 更新客户级别
     */
    @Tool(description = "更新客户级别")
    public String updateCustomerLevel(
            @ToolParam(name = "customerId", description = "客户ID") String customerId,
            @ToolParam(name = "level", description = "新级别: STRATEGIC, KEY, NORMAL") String level) {
        try {
            UUID id = UUID.fromString(customerId);
            Customer updateData = Customer.builder()
                .level(Customer.CustomerLevel.valueOf(level))
                .build();
            
            customerService.updateCustomer(id, updateData);
            return "客户级别更新成功";
        } catch (Exception e) {
            log.error("更新客户级别失败", e);
            return "更新失败: " + e.getMessage();
        }
    }

    /**
     * 设置下次跟进时间
     */
    @Tool(description = "设置客户下次跟进时间")
    public String scheduleFollowUp(
            @ToolParam(name = "customerId", description = "客户ID") String customerId,
            @ToolParam(name = "days", description = "多少天后跟进") Integer days) {
        try {
            UUID id = UUID.fromString(customerId);
            java.time.LocalDateTime followUpTime = java.time.LocalDateTime.now().plusDays(days != null ? days : 7);
            customerService.setNextFollowUp(id, followUpTime);
            return "跟进计划设置成功，将于 " + followUpTime.toLocalDate() + " 跟进";
        } catch (Exception e) {
            log.error("设置跟进时间失败", e);
            return "设置失败: " + e.getMessage();
        }
    }

    /**
     * 获取需要跟进的客户
     */
    @Tool(description = "获取需要跟进的客户列表")
    public String getCustomersNeedFollowUp() {
        try {
            var customers = customerService.getCustomersNeedFollowUp();
            if (customers.isEmpty()) {
                return "当前没有需要跟进的客户";
            }
            
            StringBuilder sb = new StringBuilder("需要跟进的客户：\n");
            for (Customer c : customers) {
                sb.append(String.format("- %s (ID: %s), 下次跟进: %s\n",
                    c.getName(), c.getId(), c.getNextFollowUpAt()));
            }
            return sb.toString();
        } catch (Exception e) {
            log.error("获取跟进列表失败", e);
            return "查询失败: " + e.getMessage();
        }
    }
}
