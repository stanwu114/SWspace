package com.aispace.service;

import com.aispace.dto.response.PageResponse;
import com.aispace.entity.Project;
import com.aispace.repository.ProjectRepository;
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

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * 项目服务层
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProjectService {
    
    private final ProjectRepository projectRepository;
    
    /**
     * 获取项目列表（分页）
     */
    public PageResponse<Project> listProjects(
            Project.ProjectStatus status,
            UUID customerId,
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
        
        Page<Project> result;
        
        if (keyword != null && !keyword.isBlank()) {
            result = projectRepository.searchByKeyword(keyword, pageable);
        } else if (status != null) {
            result = projectRepository.findByStatus(status, pageable);
        } else {
            result = projectRepository.findAll(pageable);
        }
        
        return PageResponse.of(
            result.getContent(),
            result.getTotalElements(),
            page,
            pageSize
        );
    }
    
    /**
     * 获取项目详情
     */
    @Cacheable(value = "projects", key = "#id")
    public Optional<Project> getProject(UUID id) {
        return projectRepository.findById(id);
    }
    
    /**
     * 创建项目
     */
    @Transactional
    public Project createProject(Project project) {
        log.info("Creating project: {}", project.getName());
        return projectRepository.save(project);
    }
    
    /**
     * 更新项目
     */
    @Transactional
    @CacheEvict(value = "projects", key = "#id")
    public Project updateProject(UUID id, Project projectData) {
        Project project = projectRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Project not found: " + id));
        
        // 更新字段
        if (projectData.getName() != null) {
            project.setName(projectData.getName());
        }
        if (projectData.getDescription() != null) {
            project.setDescription(projectData.getDescription());
        }
        if (projectData.getEstimatedValue() != null) {
            project.setEstimatedValue(projectData.getEstimatedValue());
        }
        if (projectData.getBidDeadline() != null) {
            project.setBidDeadline(projectData.getBidDeadline());
        }
        if (projectData.getTags() != null) {
            project.setTags(projectData.getTags());
        }
        
        log.info("Updating project: {}", id);
        return projectRepository.save(project);
    }
    
    /**
     * 更新项目状态
     */
    @Transactional
    public Project updateStatus(UUID id, Project.ProjectStatus status, String note) {
        Project project = projectRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Project not found: " + id));
        
        project.setStatus(status);
        log.info("Updating project status: {} -> {}", id, status);
        
        return projectRepository.save(project);
    }
    
    /**
     * 删除项目
     */
    @Transactional
    @CacheEvict(value = "projects", key = "#id")
    public void deleteProject(UUID id) {
        log.info("Deleting project: {}", id);
        projectRepository.deleteById(id);
    }
    
    /**
     * 获取客户的项目列表
     */
    public List<Project> getProjectsByCustomer(UUID customerId) {
        return projectRepository.findByCustomerId(customerId);
    }
    
    /**
     * 获取即将到期的项目
     */
    public List<Project> getUpcomingDeadlines(int limit) {
        return projectRepository.findUpcomingDeadlines(PageRequest.of(0, limit));
    }
}
