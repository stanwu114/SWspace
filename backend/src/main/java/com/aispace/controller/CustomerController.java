package com.aispace.controller;

import com.aispace.dto.response.ApiResponse;
import com.aispace.dto.response.PageResponse;
import com.aispace.entity.Customer;
import com.aispace.service.CustomerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * 客户管理控制器
 */
@RestController
@RequestMapping("/api/v1/customers")
@RequiredArgsConstructor
@Tag(name = "客户管理", description = "客户CRUD和关系管理接口")
public class CustomerController {
    
    private final CustomerService customerService;
    
    /**
     * 获取客户列表
     */
    @GetMapping
    @Operation(summary = "获取客户列表")
    public ApiResponse<PageResponse<Customer>> listCustomers(
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String level,
            @RequestParam(required = false) String region,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) String sort,
            @RequestParam(defaultValue = "desc") String order
    ) {
        Customer.CustomerType typeEnum = type != null 
            ? Customer.CustomerType.valueOf(type.toUpperCase()) 
            : null;
        Customer.CustomerLevel levelEnum = level != null 
            ? Customer.CustomerLevel.valueOf(level.toUpperCase()) 
            : null;
        
        PageResponse<Customer> result = customerService.listCustomers(
            typeEnum, levelEnum, region, keyword, page, pageSize, sort, order
        );
        
        return ApiResponse.success(result);
    }
    
    /**
     * 获取客户详情
     */
    @GetMapping("/{id}")
    @Operation(summary = "获取客户详情")
    public ApiResponse<Customer> getCustomer(@PathVariable UUID id) {
        return customerService.getCustomer(id)
            .map(ApiResponse::success)
            .orElse(ApiResponse.notFound("客户不存在"));
    }
    
    /**
     * 创建客户
     */
    @PostMapping
    @Operation(summary = "创建客户")
    public ApiResponse<Customer> createCustomer(@Valid @RequestBody Customer customer) {
        // 验证必填字段
        if (customer.getName() == null || customer.getName().isBlank()) {
            return ApiResponse.error(400, "客户名称不能为空");
        }
        if (customer.getType() == null) {
            return ApiResponse.error(400, "客户类型不能为空");
        }
        
        Customer created = customerService.createCustomer(customer);
        return ApiResponse.success("客户创建成功", created);
    }
    
    /**
     * 更新客户
     */
    @PutMapping("/{id}")
    @Operation(summary = "更新客户")
    public ApiResponse<Customer> updateCustomer(
            @PathVariable UUID id,
            @Valid @RequestBody Customer customer
    ) {
        Customer updated = customerService.updateCustomer(id, customer);
        return ApiResponse.success("客户更新成功", updated);
    }
    
    /**
     * 删除客户
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "删除客户")
    public ApiResponse<Void> deleteCustomer(@PathVariable UUID id) {
        customerService.deleteCustomer(id);
        return ApiResponse.success("客户删除成功", null);
    }
    
    /**
     * 获取需要跟进的客户
     */
    @GetMapping("/need-follow-up")
    @Operation(summary = "获取需要跟进的客户")
    public ApiResponse<List<Customer>> getCustomersNeedFollowUp() {
        List<Customer> customers = customerService.getCustomersNeedFollowUp();
        return ApiResponse.success(customers);
    }
    
    /**
     * 设置下次跟进时间
     */
    @PatchMapping("/{id}/follow-up")
    @Operation(summary = "设置下次跟进时间")
    public ApiResponse<Void> setFollowUp(
            @PathVariable UUID id,
            @RequestBody FollowUpRequest request
    ) {
        customerService.setNextFollowUp(id, request.followUpAt());
        return ApiResponse.success("跟进时间设置成功", null);
    }
    
    /**
     * 跟进时间请求
     */
    public record FollowUpRequest(LocalDateTime followUpAt) {}
}
