package com.aispace.repository;

import com.aispace.entity.AISession;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * AI会话数据访问层
 */
@Repository
public interface AISessionRepository extends JpaRepository<AISession, UUID> {
    
    /**
     * 根据Agent类型查询会话
     */
    Page<AISession> findByAgentTypeAndStatusOrderByUpdatedAtDesc(
        AISession.AgentType agentType, 
        AISession.SessionStatus status, 
        Pageable pageable
    );
    
    /**
     * 根据项目ID查询会话
     */
    List<AISession> findByProjectIdAndStatusOrderByUpdatedAtDesc(
        UUID projectId, 
        AISession.SessionStatus status
    );
    
    /**
     * 根据客户ID查询会话
     */
    List<AISession> findByCustomerIdAndStatusOrderByUpdatedAtDesc(
        UUID customerId, 
        AISession.SessionStatus status
    );
    
    /**
     * 查询活跃会话
     */
    Page<AISession> findByStatusOrderByUpdatedAtDesc(
        AISession.SessionStatus status, 
        Pageable pageable
    );
}
