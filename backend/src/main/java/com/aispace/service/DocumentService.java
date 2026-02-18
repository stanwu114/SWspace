package com.aispace.service;

import com.aispace.dto.response.PageResponse;
import com.aispace.entity.Document;
import com.aispace.repository.DocumentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * 文档服务层
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentService {
    
    private final DocumentRepository documentRepository;
    
    @Value("${aispace.storage.documents-path:~/.aispace/documents}")
    private String documentsPath;
    
    /**
     * 上传文档
     */
    @Transactional
    public Document uploadDocument(
            MultipartFile file,
            Document.DocumentType type,
            UUID projectId,
            UUID customerId
    ) throws IOException {
        // 生成存储路径
        String originalName = file.getOriginalFilename();
        String fileExt = getFileExtension(originalName);
        String storedName = UUID.randomUUID() + "." + fileExt;
        
        // 按日期组织目录
        String datePath = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy/MM"));
        Path targetDir = Paths.get(documentsPath.replace("~", System.getProperty("user.home")), datePath);
        Files.createDirectories(targetDir);
        
        Path targetPath = targetDir.resolve(storedName);
        file.transferTo(targetPath.toFile());
        
        // 创建文档记录
        Document document = Document.builder()
            .name(originalName)
            .originalName(originalName)
            .type(type)
            .filePath(targetPath.toString())
            .fileSize(file.getSize())
            .fileExt(fileExt)
            .mimeType(file.getContentType())
            .projectId(projectId)
            .customerId(customerId)
            .build();
        
        log.info("Uploaded document: {} -> {}", originalName, targetPath);
        return documentRepository.save(document);
    }
    
    /**
     * 获取文档列表
     */
    public PageResponse<Document> listDocuments(
            Document.DocumentType type,
            UUID projectId,
            UUID customerId,
            int page,
            int pageSize
    ) {
        Pageable pageable = PageRequest.of(page - 1, pageSize, Sort.by(Sort.Direction.DESC, "createdAt"));
        
        Page<Document> result;
        if (type != null) {
            result = documentRepository.findByTypeAndIsLatestTrue(type, pageable);
        } else {
            result = documentRepository.findAll(pageable);
        }
        
        return PageResponse.of(
            result.getContent(),
            result.getTotalElements(),
            page,
            pageSize
        );
    }
    
    /**
     * 获取文档详情
     */
    public Optional<Document> getDocument(UUID id) {
        return documentRepository.findById(id);
    }
    
    /**
     * 获取项目文档
     */
    public List<Document> getProjectDocuments(UUID projectId) {
        return documentRepository.findByProjectIdAndIsLatestTrue(projectId);
    }
    
    /**
     * 获取客户文档
     */
    public List<Document> getCustomerDocuments(UUID customerId) {
        return documentRepository.findByCustomerIdAndIsLatestTrue(customerId);
    }
    
    /**
     * 更新文档AI分析结果
     */
    @Transactional
    public void updateAIAnalysis(UUID id, java.util.Map<String, Object> analysis, String summary) {
        Document document = documentRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Document not found: " + id));
        document.setAiAnalysis(analysis);
        document.setAiSummary(summary);
        documentRepository.save(document);
    }
    
    /**
     * 删除文档
     */
    @Transactional
    public void deleteDocument(UUID id) {
        Document document = documentRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Document not found: " + id));
        
        // 删除文件
        try {
            Files.deleteIfExists(Paths.get(document.getFilePath()));
        } catch (IOException e) {
            log.warn("Failed to delete file: {}", document.getFilePath(), e);
        }
        
        documentRepository.deleteById(id);
        log.info("Deleted document: {}", id);
    }
    
    private String getFileExtension(String filename) {
        if (filename == null) return "";
        int lastDot = filename.lastIndexOf('.');
        return lastDot > 0 ? filename.substring(lastDot + 1).toLowerCase() : "";
    }
}
