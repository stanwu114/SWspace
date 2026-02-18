package com.aispace.repository;

import com.aispace.entity.Document;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * 文档数据访问层
 */
@Repository
public interface DocumentRepository extends JpaRepository<Document, UUID> {
    
    /**
     * 根据项目ID查询文档
     */
    List<Document> findByProjectIdAndIsLatestTrue(UUID projectId);
    
    /**
     * 根据客户ID查询文档
     */
    List<Document> findByCustomerIdAndIsLatestTrue(UUID customerId);
    
    /**
     * 根据类型查询文档
     */
    Page<Document> findByTypeAndIsLatestTrue(Document.DocumentType type, Pageable pageable);
    
    /**
     * 查询文档版本历史
     */
    List<Document> findByParentIdOrderByVersionDesc(UUID parentId);
}
