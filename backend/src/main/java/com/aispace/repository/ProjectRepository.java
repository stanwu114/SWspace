package com.aispace.repository;

import com.aispace.entity.Project;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * 项目数据访问层
 */
@Repository
public interface ProjectRepository extends JpaRepository<Project, UUID> {
    
    /**
     * 根据状态查询项目列表
     */
    Page<Project> findByStatus(Project.ProjectStatus status, Pageable pageable);
    
    /**
     * 根据客户ID查询项目列表
     */
    List<Project> findByCustomerId(UUID customerId);
    
    /**
     * 关键词搜索项目
     */
    @Query("SELECT p FROM Project p WHERE " +
           "LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(p.description) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    Page<Project> searchByKeyword(@Param("keyword") String keyword, Pageable pageable);
    
    /**
     * 统计各状态项目数量
     */
    @Query("SELECT p.status, COUNT(p) FROM Project p GROUP BY p.status")
    List<Object[]> countByStatus();
    
    /**
     * 查询即将到期的项目
     */
    @Query("SELECT p FROM Project p WHERE p.bidDeadline IS NOT NULL " +
           "AND p.bidDeadline >= CURRENT_TIMESTAMP " +
           "AND p.status IN ('LEAD', 'OPPORTUNITY') " +
           "ORDER BY p.bidDeadline ASC")
    List<Project> findUpcomingDeadlines(Pageable pageable);
}
