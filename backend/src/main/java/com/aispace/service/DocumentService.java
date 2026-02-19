package com.aispace.service;

import com.aispace.dto.response.PageResponse;
import com.aispace.entity.Document;
import com.aispace.repository.DocumentRepository;
import com.aispace.agent.service.AgentManagerService;
import com.aispace.agent.agents.AISpaceAgent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.scheduling.annotation.Async;
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
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * 文档服务层
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentService {
    
    private final DocumentRepository documentRepository;
    private final AgentManagerService agentManagerService;
    
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
    
    /**
     * 异步分析文档
     */
    @Async
    public CompletableFuture<Void> analyzeDocumentAsync(UUID documentId) {
        try {
            log.info("开始分析文档: {}", documentId);
            
            // 获取文档
            Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new RuntimeException("Document not found: " + documentId));
            
            // 读取文件内容
            String content = readFileContent(document.getFilePath());
            if (content == null || content.isEmpty()) {
                log.warn("文档内容为空: {}", documentId);
                return CompletableFuture.completedFuture(null);
            }
            
            // 调用 DocAgent 进行分析
            AISpaceAgent docAgent = agentManagerService.getAgent("doc");
            String prompt = String.format("""
                请分析以下%s文档内容：
                
                文档名称：%s
                文档类型：%s
                
                内容：
                %s
                
                请提供：
                1. 文档摘要（200字以内）
                2. 关键信息提取
                3. 重要内容标记
                4. 文档质量和完整性评估
                """, 
                document.getType(), document.getName(), document.getType(), content);
            
            String analysisResult = docAgent.chat(prompt);
            
            // 更新分析结果
            updateAIAnalysis(documentId, 
                Map.of("analysis", analysisResult, "timestamp", System.currentTimeMillis()),
                analysisResult.contains("摘要") ? extractSummary(analysisResult) : "分析完成"
            );
            
            log.info("文档分析完成: {}", documentId);
            return CompletableFuture.completedFuture(null);
            
        } catch (Exception e) {
            log.error("文档分析失败: {}", documentId, e);
            return CompletableFuture.failedFuture(e);
        }
    }
    
    /**
     * 读取文件内容
     */
    private String readFileContent(String filePath) {
        try {
            Path path = Paths.get(filePath);
            if (!Files.exists(path)) {
                return null;
            }
            
            // 根据文件扩展名决定读取方式
            String ext = getFileExtension(filePath).toLowerCase();
            if (List.of("txt", "md", "csv").contains(ext)) {
                return Files.readString(path);
            } else if (List.of("pdf", "doc", "docx").contains(ext)) {
                return parseDocument(path, ext);
            } else {
                return Files.readString(path);
            }
        } catch (IOException e) {
            log.error("读取文件失败: {}", filePath, e);
            return null;
        }
    }
    
    /**
     * 从分析结果中提取摘要
     */
    private String extractSummary(String analysisResult) {
        // 简单的摘要提取逻辑
        if (analysisResult.contains("摘要")) {
            int startIndex = analysisResult.indexOf("摘要") + 2;
            int endIndex = Math.min(startIndex + 200, analysisResult.length());
            return analysisResult.substring(startIndex, endIndex).trim();
        }
        return analysisResult.substring(0, Math.min(200, analysisResult.length()));
    }
    
    /**
     * 解析文档内容（PDF、Word等格式）
     */
    private String parseDocument(Path path, String extension) {
        try {
            switch (extension.toLowerCase()) {
                case "pdf":
                    return parsePdfDocument(path);
                case "doc":
                case "docx":
                    return parseWordDocument(path);
                default:
                    return Files.readString(path);
            }
        } catch (Exception e) {
            log.error("解析文档失败: {}, extension: {}", path, extension, e);
            return "[文档解析失败: " + e.getMessage() + "]";
        }
    }
    
    /**
     * 解析PDF文档
     */
    private String parsePdfDocument(Path path) throws Exception {
        // 注意：这里使用简单的文本提取，实际项目中应该使用Apache PDFBox或其他专业库
        log.warn("PDF解析功能需要引入Apache PDFBox依赖");
        return "[PDF文档内容需要专业PDF解析库处理]";
    }
    
    /**
     * 解析Word文档
     */
    private String parseWordDocument(Path path) throws Exception {
        // 注意：这里使用简单的文本提取，实际项目中应该使用Apache POI或其他专业库
        log.warn("Word解析功能需要引入Apache POI依赖");
        return "[Word文档内容需要专业Office文档解析库处理]";
    }
}
