package com.aispace.service;

import com.aispace.dto.response.PageResponse;
import com.aispace.entity.Contact;
import com.aispace.repository.ContactRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * 联系人服务层
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ContactService {
    
    private final ContactRepository contactRepository;
    
    /**
     * 获取客户的联系人列表
     */
    public List<Contact> listByCustomer(UUID customerId) {
        return contactRepository.findByCustomerIdAndIsActiveTrue(customerId);
    }
    
    /**
     * 分页获取联系人
     */
    public PageResponse<Contact> listByCustomer(UUID customerId, int page, int pageSize) {
        Pageable pageable = PageRequest.of(page - 1, pageSize);
        Page<Contact> result = contactRepository.findByCustomerIdAndIsActiveTrue(customerId, pageable);
        
        return PageResponse.of(
            result.getContent(),
            result.getTotalElements(),
            page,
            pageSize
        );
    }
    
    /**
     * 搜索联系人
     */
    public PageResponse<Contact> search(String keyword, int page, int pageSize) {
        Pageable pageable = PageRequest.of(page - 1, pageSize);
        Page<Contact> result = contactRepository.searchByKeyword(keyword, pageable);
        
        return PageResponse.of(
            result.getContent(),
            result.getTotalElements(),
            page,
            pageSize
        );
    }
    
    /**
     * 获取联系人详情
     */
    public Optional<Contact> getContact(UUID id) {
        return contactRepository.findById(id);
    }
    
    /**
     * 创建联系人
     */
    @Transactional
    public Contact createContact(Contact contact) {
        log.info("Creating contact: {} for customer: {}", contact.getName(), contact.getCustomerId());
        return contactRepository.save(contact);
    }
    
    /**
     * 更新联系人
     */
    @Transactional
    public Contact updateContact(UUID id, Contact contactData) {
        Contact contact = contactRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Contact not found: " + id));
        
        if (contactData.getName() != null) {
            contact.setName(contactData.getName());
        }
        if (contactData.getTitle() != null) {
            contact.setTitle(contactData.getTitle());
        }
        if (contactData.getDepartment() != null) {
            contact.setDepartment(contactData.getDepartment());
        }
        if (contactData.getRole() != null) {
            contact.setRole(contactData.getRole());
        }
        if (contactData.getImportance() != null) {
            contact.setImportance(contactData.getImportance());
        }
        if (contactData.getPhone() != null) {
            contact.setPhone(contactData.getPhone());
        }
        if (contactData.getMobile() != null) {
            contact.setMobile(contactData.getMobile());
        }
        if (contactData.getEmail() != null) {
            contact.setEmail(contactData.getEmail());
        }
        if (contactData.getWechat() != null) {
            contact.setWechat(contactData.getWechat());
        }
        if (contactData.getBirthday() != null) {
            contact.setBirthday(contactData.getBirthday());
        }
        if (contactData.getPreferences() != null) {
            contact.setPreferences(contactData.getPreferences());
        }
        if (contactData.getNotes() != null) {
            contact.setNotes(contactData.getNotes());
        }
        
        log.info("Updating contact: {}", id);
        return contactRepository.save(contact);
    }
    
    /**
     * 删除联系人（软删除）
     */
    @Transactional
    public void deleteContact(UUID id) {
        Contact contact = contactRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Contact not found: " + id));
        contact.setIsActive(false);
        contactRepository.save(contact);
        log.info("Soft deleted contact: {}", id);
    }
    
    /**
     * 获取关键联系人
     */
    public List<Contact> getKeyContacts(UUID customerId) {
        return contactRepository.findKeyContacts(customerId);
    }
    
    /**
     * 根据角色获取联系人
     */
    public List<Contact> getByRole(UUID customerId, Contact.ContactRole role) {
        return contactRepository.findByCustomerIdAndRoleAndIsActiveTrue(customerId, role);
    }
    
    /**
     * 统计联系人数量
     */
    public long countByCustomer(UUID customerId) {
        return contactRepository.countByCustomerIdAndIsActiveTrue(customerId);
    }
}
