package com.aispace.service;

import com.aispace.entity.Reminder;
import com.aispace.repository.ReminderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * 提醒服务层
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReminderService {
    
    private final ReminderRepository reminderRepository;
    
    /**
     * 创建提醒
     */
    @Transactional
    public Reminder createReminder(Reminder reminder) {
        log.info("Creating reminder: {}", reminder.getTitle());
        return reminderRepository.save(reminder);
    }
    
    /**
     * 获取提醒详情
     */
    public Optional<Reminder> getReminder(UUID id) {
        return reminderRepository.findById(id);
    }
    
    /**
     * 获取今日提醒
     */
    public List<Reminder> getTodayReminders() {
        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        LocalDateTime endOfDay = LocalDate.now().atTime(LocalTime.MAX);
        return reminderRepository.findTodayReminders(startOfDay, endOfDay);
    }
    
    /**
     * 获取即将到期的提醒
     */
    public List<Reminder> getUpcomingReminders(int hours, int limit) {
        LocalDateTime deadline = LocalDateTime.now().plusHours(hours);
        return reminderRepository.findUpcomingReminders(deadline, PageRequest.of(0, limit));
    }
    
    /**
     * 获取项目提醒
     */
    public List<Reminder> getProjectReminders(UUID projectId) {
        return reminderRepository.findByProjectIdAndStatusOrderByRemindAtAsc(
            projectId, Reminder.ReminderStatus.PENDING
        );
    }
    
    /**
     * 获取客户提醒
     */
    public List<Reminder> getCustomerReminders(UUID customerId) {
        return reminderRepository.findByCustomerIdAndStatusOrderByRemindAtAsc(
            customerId, Reminder.ReminderStatus.PENDING
        );
    }
    
    /**
     * 完成提醒
     */
    @Transactional
    public void completeReminder(UUID id) {
        Reminder reminder = reminderRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Reminder not found: " + id));
        reminder.setStatus(Reminder.ReminderStatus.DONE);
        reminderRepository.save(reminder);
        log.info("Completed reminder: {}", id);
    }
    
    /**
     * 忽略提醒
     */
    @Transactional
    public void dismissReminder(UUID id) {
        Reminder reminder = reminderRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Reminder not found: " + id));
        reminder.setStatus(Reminder.ReminderStatus.DISMISSED);
        reminderRepository.save(reminder);
        log.info("Dismissed reminder: {}", id);
    }
    
    /**
     * 推迟提醒
     */
    @Transactional
    public void snoozeReminder(UUID id, int minutes) {
        Reminder reminder = reminderRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Reminder not found: " + id));
        reminder.setStatus(Reminder.ReminderStatus.SNOOZED);
        reminder.setSnoozedUntil(LocalDateTime.now().plusMinutes(minutes));
        reminderRepository.save(reminder);
        log.info("Snoozed reminder: {} for {} minutes", id, minutes);
    }
    
    /**
     * 删除提醒
     */
    @Transactional
    public void deleteReminder(UUID id) {
        reminderRepository.deleteById(id);
        log.info("Deleted reminder: {}", id);
    }
}
