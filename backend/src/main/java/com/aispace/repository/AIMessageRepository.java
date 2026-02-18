package com.aispace.repository;

import com.aispace.entity.AIMessage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * AI消息数据访问层
 */
@Repository
public interface AIMessageRepository extends JpaRepository<AIMessage, UUID> {
    
    /**
     * 根据会话ID查询消息
     */
    List<AIMessage> findBySessionIdOrderByCreatedAtAsc(UUID sessionId);
    
    /**
     * 分页查询会话消息
     */
    Page<AIMessage> findBySessionIdOrderByCreatedAtDesc(UUID sessionId, Pageable pageable);
    
    /**
     * 统计会话消息数量
     */
    long countBySessionId(UUID sessionId);
    
    /**
     * 删除会话的所有消息
     */
    void deleteBySessionId(UUID sessionId);
}
