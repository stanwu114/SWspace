package com.aispace.controller;

import com.aispace.dto.response.ApiResponse;
import com.aispace.dto.response.PageResponse;
import com.aispace.entity.Document;
import com.aispace.service.DocumentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

/**
 * 文档管理控制器
 */
@RestController
@RequestMapping("/api/v1/documents")
@RequiredArgsConstructor
@Tag(name = "文档管理", description = "文档上传下载和AI分析接口")
public class DocumentController {
    
    private final DocumentService documentService;
    
    /**
     * 上传文档
     */
    @PostMapping("/upload")
    @Operation(summary = "上传文档")
    public ApiResponse<Document> uploadDocument(
            @RequestParam("file") MultipartFile file,
            @RequestParam(defaultValue = "OTHER") String type,
            @RequestParam(required = false) UUID projectId,
            @RequestParam(required = false) UUID customerId
    ) throws IOException {
        Document.DocumentType docType = Document.DocumentType.valueOf(type.toUpperCase());
        Document document = documentService.uploadDocument(file, docType, projectId, customerId);
        return ApiResponse.success("文档上传成功", document);
    }
    
    /**
     * 获取文档列表
     */
    @GetMapping
    @Operation(summary = "获取文档列表")
    public ApiResponse<PageResponse<Document>> listDocuments(
            @RequestParam(required = false) String type,
            @RequestParam(required = false) UUID projectId,
            @RequestParam(required = false) UUID customerId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize
    ) {
        Document.DocumentType docType = type != null 
            ? Document.DocumentType.valueOf(type.toUpperCase()) 
            : null;
        
        PageResponse<Document> result = documentService.listDocuments(
            docType, projectId, customerId, page, pageSize
        );
        return ApiResponse.success(result);
    }
    
    /**
     * 获取文档详情
     */
    @GetMapping("/{id}")
    @Operation(summary = "获取文档详情")
    public ApiResponse<Document> getDocument(@PathVariable UUID id) {
        return documentService.getDocument(id)
            .map(ApiResponse::success)
            .orElse(ApiResponse.notFound("文档不存在"));
    }
    
    /**
     * 下载文档
     */
    @GetMapping("/{id}/download")
    @Operation(summary = "下载文档")
    public ResponseEntity<Resource> downloadDocument(@PathVariable UUID id) {
        Document document = documentService.getDocument(id)
            .orElseThrow(() -> new RuntimeException("Document not found: " + id));
        
        Path filePath = Path.of(document.getFilePath());
        Resource resource = new FileSystemResource(filePath);
        
        String encodedName = URLEncoder.encode(document.getName(), StandardCharsets.UTF_8)
            .replaceAll("\\+", "%20");
        
        return ResponseEntity.ok()
            .contentType(MediaType.APPLICATION_OCTET_STREAM)
            .header(HttpHeaders.CONTENT_DISPOSITION, 
                "attachment; filename*=UTF-8''" + encodedName)
            .body(resource);
    }
    
    /**
     * 获取项目文档
     */
    @GetMapping("/project/{projectId}")
    @Operation(summary = "获取项目文档")
    public ApiResponse<List<Document>> getProjectDocuments(@PathVariable UUID projectId) {
        List<Document> documents = documentService.getProjectDocuments(projectId);
        return ApiResponse.success(documents);
    }
    
    /**
     * 获取客户文档
     */
    @GetMapping("/customer/{customerId}")
    @Operation(summary = "获取客户文档")
    public ApiResponse<List<Document>> getCustomerDocuments(@PathVariable UUID customerId) {
        List<Document> documents = documentService.getCustomerDocuments(customerId);
        return ApiResponse.success(documents);
    }
    
    /**
     * 请求AI分析
     */
    @PostMapping("/{id}/analyze")
    @Operation(summary = "请求AI分析文档")
    public ApiResponse<String> analyzeDocument(@PathVariable UUID id) {
        documentService.analyzeDocumentAsync(id);
        return ApiResponse.success("AI分析任务已创建，请稍后查看结果", null);
    }
    
    /**
     * 删除文档
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "删除文档")
    public ApiResponse<Void> deleteDocument(@PathVariable UUID id) {
        documentService.deleteDocument(id);
        return ApiResponse.success("文档删除成功", null);
    }
}
