package com.aispace.service;

import com.aispace.dto.response.PageResponse;
import com.aispace.entity.Customer;
import com.aispace.repository.CustomerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * 客户服务层
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CustomerService {
    
    private final CustomerRepository customerRepository;
    
    /**
     * 获取客户列表（分页）
     */
    public PageResponse<Customer> listCustomers(
            Customer.CustomerType type,
            Customer.CustomerLevel level,
            String region,
            String keyword,
            int page,
            int pageSize,
            String sortBy,
            String sortOrder
    ) {
        Sort sort = Sort.by(
            "desc".equalsIgnoreCase(sortOrder) ? Sort.Direction.DESC : Sort.Direction.ASC,
            sortBy != null ? sortBy : "createdAt"
        );
        Pageable pageable = PageRequest.of(page - 1, pageSize, sort);
        
        Page<Customer> result;
        
        if (keyword != null && !keyword.isBlank()) {
            result = customerRepository.searchByKeyword(keyword, pageable);
        } else if (type != null) {
            result = customerRepository.findByType(type, pageable);
        } else if (level != null) {
            result = customerRepository.findByLevel(level, pageable);
        } else if (region != null && !region.isBlank()) {
            result = customerRepository.findByRegion(region, pageable);
        } else {
            result = customerRepository.findAll(pageable);
        }
        
        return PageResponse.of(
            result.getContent(),
            result.getTotalElements(),
            page,
            pageSize
        );
    }
    
    /**
     * 获取客户详情
     */
    @Cacheable(value = "customers", key = "#id")
    public Optional<Customer> getCustomer(UUID id) {
        return customerRepository.findById(id);
    }
    
    /**
     * 创建客户
     */
    @Transactional
    public Customer createCustomer(Customer customer) {
        log.info("Creating customer: {}", customer.getName());
        return customerRepository.save(customer);
    }
    
    /**
     * 更新客户
     */
    @Transactional
    @CacheEvict(value = "customers", key = "#id")
    public Customer updateCustomer(UUID id, Customer customerData) {
        Customer customer = customerRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Customer not found: " + id));
        
        // 更新所有可更新字段
        if (customerData.getName() != null) {
            customer.setName(customerData.getName());
        }
        if (customerData.getShortName() != null) {
            customer.setShortName(customerData.getShortName());
        }
        if (customerData.getType() != null) {
            customer.setType(customerData.getType());
        }
        if (customerData.getIndustry() != null) {
            customer.setIndustry(customerData.getIndustry());
        }
        if (customerData.getRegion() != null) {
            customer.setRegion(customerData.getRegion());
        }
        if (customerData.getAddress() != null) {
            customer.setAddress(customerData.getAddress());
        }
        if (customerData.getLevel() != null) {
            customer.setLevel(customerData.getLevel());
        }
        if (customerData.getOrgStructure() != null) {
            customer.setOrgStructure(customerData.getOrgStructure());
        }
        if (customerData.getRelationshipScore() != null) {
            customer.setRelationshipScore(customerData.getRelationshipScore());
        }
        if (customerData.getTags() != null) {
            customer.setTags(customerData.getTags());
        }
        if (customerData.getNotes() != null) {
            customer.setNotes(customerData.getNotes());
        }
        if (customerData.getWebsite() != null) {
            customer.setWebsite(customerData.getWebsite());
        }
        
        log.info("Updating customer: {}", id);
        return customerRepository.save(customer);
    }
    
    /**
     * 删除客户
     */
    @Transactional
    @CacheEvict(value = "customers", key = "#id")
    public void deleteCustomer(UUID id) {
        log.info("Deleting customer: {}", id);
        customerRepository.deleteById(id);
    }
    
    /**
     * 更新最后联系时间
     */
    @Transactional
    public void updateLastContact(UUID id) {
        Customer customer = customerRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Customer not found: " + id));
        customer.setLastContactAt(LocalDateTime.now());
        customerRepository.save(customer);
    }
    
    /**
     * 设置下次跟进时间
     */
    @Transactional
    public void setNextFollowUp(UUID id, LocalDateTime followUpAt) {
        Customer customer = customerRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Customer not found: " + id));
        customer.setNextFollowUpAt(followUpAt);
        customerRepository.save(customer);
    }
    
    /**
     * 获取需要跟进的客户
     */
    public List<Customer> getCustomersNeedFollowUp() {
        return customerRepository.findNeedFollowUp(LocalDateTime.now().plusDays(3));
    }
}
