package com.aispace.controller;

import com.aispace.dto.response.ApiResponse;
import com.aispace.dto.response.PageResponse;
import com.aispace.entity.Contact;
import com.aispace.service.ContactService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * 联系人控制器
 */
@RestController
@RequestMapping("/api/v1/contacts")
@RequiredArgsConstructor
@Tag(name = "联系人管理", description = "客户联系人CRUD接口")
public class ContactController {
    
    private final ContactService contactService;
    
    /**
     * 获取客户的联系人列表
     */
    @GetMapping("/customer/{customerId}")
    @Operation(summary = "获取客户联系人列表")
    public ApiResponse<PageResponse<Contact>> listByCustomer(
            @PathVariable UUID customerId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize
    ) {
        PageResponse<Contact> result = contactService.listByCustomer(customerId, page, pageSize);
        return ApiResponse.success(result);
    }
    
    /**
     * 搜索联系人
     */
    @GetMapping("/search")
    @Operation(summary = "搜索联系人")
    public ApiResponse<PageResponse<Contact>> search(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize
    ) {
        PageResponse<Contact> result = contactService.search(keyword, page, pageSize);
        return ApiResponse.success(result);
    }
    
    /**
     * 获取联系人详情
     */
    @GetMapping("/{id}")
    @Operation(summary = "获取联系人详情")
    public ApiResponse<Contact> getContact(@PathVariable UUID id) {
        return contactService.getContact(id)
            .map(ApiResponse::success)
            .orElse(ApiResponse.notFound("联系人不存在"));
    }
    
    /**
     * 创建联系人
     */
    @PostMapping
    @Operation(summary = "创建联系人")
    public ApiResponse<Contact> createContact(@RequestBody Contact contact) {
        Contact created = contactService.createContact(contact);
        return ApiResponse.success("联系人创建成功", created);
    }
    
    /**
     * 更新联系人
     */
    @PutMapping("/{id}")
    @Operation(summary = "更新联系人")
    public ApiResponse<Contact> updateContact(
            @PathVariable UUID id,
            @RequestBody Contact contact
    ) {
        Contact updated = contactService.updateContact(id, contact);
        return ApiResponse.success("联系人更新成功", updated);
    }
    
    /**
     * 删除联系人
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "删除联系人")
    public ApiResponse<Void> deleteContact(@PathVariable UUID id) {
        contactService.deleteContact(id);
        return ApiResponse.success("联系人删除成功", null);
    }
    
    /**
     * 获取关键联系人
     */
    @GetMapping("/customer/{customerId}/key")
    @Operation(summary = "获取关键联系人")
    public ApiResponse<List<Contact>> getKeyContacts(@PathVariable UUID customerId) {
        List<Contact> contacts = contactService.getKeyContacts(customerId);
        return ApiResponse.success(contacts);
    }
    
    /**
     * 按角色获取联系人
     */
    @GetMapping("/customer/{customerId}/role/{role}")
    @Operation(summary = "按角色获取联系人")
    public ApiResponse<List<Contact>> getByRole(
            @PathVariable UUID customerId,
            @PathVariable String role
    ) {
        Contact.ContactRole contactRole = Contact.ContactRole.valueOf(role.toUpperCase());
        List<Contact> contacts = contactService.getByRole(customerId, contactRole);
        return ApiResponse.success(contacts);
    }
}
