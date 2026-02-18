package com.aispace.repository;

import com.aispace.entity.Reminder;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * 提醒数据访问层
 */
@Repository
public interface ReminderRepository extends JpaRepository<Reminder, UUID> {
    
    /**
     * 查询待处理的提醒
     */
    List<Reminder> findByStatusAndRemindAtBeforeOrderByRemindAtAsc(
        Reminder.ReminderStatus status, 
        LocalDateTime before
    );
    
    /**
     * 查询今日提醒
     */
    @Query("SELECT r FROM Reminder r WHERE r.status = 'PENDING' " +
           "AND r.remindAt >= :startOfDay AND r.remindAt < :endOfDay " +
           "ORDER BY r.remindAt ASC")
    List<Reminder> findTodayReminders(
        @Param("startOfDay") LocalDateTime startOfDay,
        @Param("endOfDay") LocalDateTime endOfDay
    );
    
    /**
     * 根据项目查询提醒
     */
    List<Reminder> findByProjectIdAndStatusOrderByRemindAtAsc(
        UUID projectId, 
        Reminder.ReminderStatus status
    );
    
    /**
     * 根据客户查询提醒
     */
    List<Reminder> findByCustomerIdAndStatusOrderByRemindAtAsc(
        UUID customerId, 
        Reminder.ReminderStatus status
    );
    
    /**
     * 查询即将到期的提醒
     */
    @Query("SELECT r FROM Reminder r WHERE r.status = 'PENDING' " +
           "AND r.remindAt <= :deadline ORDER BY r.remindAt ASC")
    List<Reminder> findUpcomingReminders(
        @Param("deadline") LocalDateTime deadline, 
        Pageable pageable
    );
}
