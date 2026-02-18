package com.aispace.repository;

import com.aispace.entity.Contact;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * 联系人数据访问层
 */
@Repository
public interface ContactRepository extends JpaRepository<Contact, UUID> {
    
    /**
     * 根据客户ID查询联系人
     */
    List<Contact> findByCustomerIdAndIsActiveTrue(UUID customerId);
    
    /**
     * 分页查询客户的联系人
     */
    Page<Contact> findByCustomerIdAndIsActiveTrue(UUID customerId, Pageable pageable);
    
    /**
     * 根据角色查询
     */
    List<Contact> findByCustomerIdAndRoleAndIsActiveTrue(UUID customerId, Contact.ContactRole role);
    
    /**
     * 根据重要性查询
     */
    List<Contact> findByCustomerIdAndImportanceAndIsActiveTrue(UUID customerId, Contact.ContactImportance importance);
    
    /**
     * 搜索联系人
     */
    @Query("SELECT c FROM Contact c WHERE c.isActive = true AND " +
           "(LOWER(c.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(c.email) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "c.phone LIKE CONCAT('%', :keyword, '%') OR " +
           "c.mobile LIKE CONCAT('%', :keyword, '%'))")
    Page<Contact> searchByKeyword(@Param("keyword") String keyword, Pageable pageable);
    
    /**
     * 查询关键决策者
     */
    @Query("SELECT c FROM Contact c WHERE c.customerId = :customerId AND c.isActive = true " +
           "AND (c.role = 'DECISION_MAKER' OR c.importance = 'KEY')")
    List<Contact> findKeyContacts(@Param("customerId") UUID customerId);
    
    /**
     * 统计客户联系人数量
     */
    long countByCustomerIdAndIsActiveTrue(UUID customerId);
}
