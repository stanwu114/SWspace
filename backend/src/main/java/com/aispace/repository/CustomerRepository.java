package com.aispace.repository;

import com.aispace.entity.Customer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * 客户数据访问层
 */
@Repository
public interface CustomerRepository extends JpaRepository<Customer, UUID> {
    
    /**
     * 根据客户类型查询
     */
    Page<Customer> findByType(Customer.CustomerType type, Pageable pageable);
    
    /**
     * 根据客户级别查询
     */
    Page<Customer> findByLevel(Customer.CustomerLevel level, Pageable pageable);
    
    /**
     * 根据地区查询
     */
    Page<Customer> findByRegion(String region, Pageable pageable);
    
    /**
     * 关键词搜索客户
     */
    @Query("SELECT c FROM Customer c WHERE " +
           "LOWER(c.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(c.shortName) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    Page<Customer> searchByKeyword(@Param("keyword") String keyword, Pageable pageable);
    
    /**
     * 查询需要跟进的客户
     */
    @Query("SELECT c FROM Customer c WHERE c.nextFollowUpAt IS NOT NULL " +
           "AND c.nextFollowUpAt <= :deadline " +
           "ORDER BY c.nextFollowUpAt ASC")
    List<Customer> findNeedFollowUp(@Param("deadline") LocalDateTime deadline);
    
    /**
     * 统计各级别客户数量
     */
    @Query("SELECT c.level, COUNT(c) FROM Customer c GROUP BY c.level")
    List<Object[]> countByLevel();
}
