package com.aispace.service;

import com.aispace.entity.Knowledge;
import com.aispace.repository.KnowledgeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * RAG检索增强服务
 * 实现语义检索、多路召回、知识融合
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RAGService {
    
    private final KnowledgeRepository knowledgeRepository;
    private final JdbcTemplate jdbcTemplate;
    
    private static final int DEFAULT_TOP_K = 5;
    private static final double DEFAULT_MIN_SCORE = 0.6;
    
    /**
     * 语义搜索 - 基于向量相似度
     * 注：需要配置 Embedding 模型才能使用
     */
    public List<SearchResult> semanticSearch(String query, int topK, double minScore) {
        log.info("Semantic search: query={}, topK={}", query, topK);
        
        // 暂时回退到关键词搜索，直到配置 Embedding 服务
        log.warn("Semantic search requires embedding model, falling back to keyword search");
        return keywordSearch(query, topK);
    }
    
    /**
     * 关键词搜索
     */
    public List<SearchResult> keywordSearch(String query, int topK) {
        log.info("Keyword search: query={}", query);
        
        String sql = """
            SELECT id, title, content, summary, category,
                   ts_rank(to_tsvector('simple', title || ' ' || content), 
                           plainto_tsquery('simple', ?)) as rank
            FROM knowledge
            WHERE to_tsvector('simple', title || ' ' || content) @@ plainto_tsquery('simple', ?)
               OR title ILIKE ? OR content ILIKE ?
            ORDER BY rank DESC
            LIMIT ?
            """;
        
        String likePattern = "%" + query + "%";
        
        try {
            return jdbcTemplate.query(sql,
                (rs, rowNum) -> SearchResult.builder()
                    .id(UUID.fromString(rs.getString("id")))
                    .title(rs.getString("title"))
                    .content(rs.getString("content"))
                    .summary(rs.getString("summary"))
                    .category(rs.getString("category"))
                    .score(rs.getDouble("rank"))
                    .matchType("keyword")
                    .build(),
                query, query, likePattern, likePattern, topK
            );
        } catch (Exception e) {
            log.error("Keyword search failed", e);
            return Collections.emptyList();
        }
    }
    
    /**
     * 混合检索 - 多路召回融合
     * 结合语义检索和关键词检索的结果
     */
    public List<SearchResult> hybridSearch(String query, int topK, double minScore) {
        log.info("Hybrid search: query={}", query);
        
        // 1. 语义检索
        List<SearchResult> semanticResults = semanticSearch(query, topK, minScore);
        
        // 2. 关键词检索
        List<SearchResult> keywordResults = keywordSearch(query, topK);
        
        // 3. 结果融合（RRF - Reciprocal Rank Fusion）
        Map<UUID, SearchResult> fusedResults = new LinkedHashMap<>();
        
        // 添加语义结果，权重0.7
        for (int i = 0; i < semanticResults.size(); i++) {
            SearchResult result = semanticResults.get(i);
            double rrfScore = 0.7 / (60 + i + 1);
            result.setFusedScore(rrfScore);
            fusedResults.put(result.getId(), result);
        }
        
        // 添加关键词结果，权重0.3
        for (int i = 0; i < keywordResults.size(); i++) {
            SearchResult result = keywordResults.get(i);
            double rrfScore = 0.3 / (60 + i + 1);
            
            if (fusedResults.containsKey(result.getId())) {
                // 已存在，累加分数
                SearchResult existing = fusedResults.get(result.getId());
                existing.setFusedScore(existing.getFusedScore() + rrfScore);
                existing.setMatchType("hybrid");
            } else {
                result.setFusedScore(rrfScore);
                fusedResults.put(result.getId(), result);
            }
        }
        
        // 4. 按融合分数排序
        return fusedResults.values().stream()
            .sorted(Comparator.comparingDouble(SearchResult::getFusedScore).reversed())
            .limit(topK)
            .collect(Collectors.toList());
    }
    
    /**
     * 按分类检索
     */
    public List<SearchResult> searchByCategory(String query, 
            List<Knowledge.KnowledgeCategory> categories, int topK) {
        
        List<SearchResult> results = hybridSearch(query, topK * 2, DEFAULT_MIN_SCORE);
        
        if (categories != null && !categories.isEmpty()) {
            Set<String> categoryNames = categories.stream()
                .map(Enum::name)
                .collect(Collectors.toSet());
            
            results = results.stream()
                .filter(r -> categoryNames.contains(r.getCategory()))
                .collect(Collectors.toList());
        }
        
        return results.stream().limit(topK).collect(Collectors.toList());
    }
    
    /**
     * 相关知识推荐
     * 基于当前知识内容推荐相关内容
     */
    public List<SearchResult> getRelatedKnowledge(UUID knowledgeId, int topK) {
        Optional<Knowledge> knowledge = knowledgeRepository.findById(knowledgeId);
        if (knowledge.isEmpty()) {
            return Collections.emptyList();
        }
        
        Knowledge k = knowledge.get();
        String query = k.getTitle() + " " + (k.getSummary() != null ? k.getSummary() : "");
        
        return semanticSearch(query, topK + 1, DEFAULT_MIN_SCORE).stream()
            .filter(r -> !r.getId().equals(knowledgeId))
            .limit(topK)
            .collect(Collectors.toList());
    }
    
    /**
     * 为知识生成并存储向量
     * 注：需要配置 Embedding 模型才能使用
     */
    @Transactional
    public void generateEmbedding(UUID knowledgeId) {
        log.warn("Embedding generation requires embedding model configuration");
    }
    
    /**
     * 批量生成向量
     * 注：需要配置 Embedding 模型才能使用
     */
    @Transactional
    public int generateEmbeddingsForAll() {
        log.warn("Embedding generation requires embedding model configuration");
        return 0;
    }
    
    /**
     * 构建RAG上下文
     * 根据查询检索相关知识，构建增强上下文
     */
    public String buildRAGContext(String query, int maxItems) {
        List<SearchResult> results = hybridSearch(query, maxItems, DEFAULT_MIN_SCORE);
        
        if (results.isEmpty()) {
            return "";
        }
        
        StringBuilder context = new StringBuilder();
        context.append("以下是与您查询相关的知识库内容：\n\n");
        
        for (int i = 0; i < results.size(); i++) {
            SearchResult result = results.get(i);
            context.append(String.format("【知识%d】%s\n", i + 1, result.getTitle()));
            context.append(result.getSummary() != null ? result.getSummary() : truncate(result.getContent(), 500));
            context.append("\n\n");
        }
        
        return context.toString();
    }
    
    /**
     * 智能问答（带RAG）
     * 注：需要配置 LLM 服务才能使用
     */
    public String askWithRAG(String question) {
        String context = buildRAGContext(question, 3);
        
        if (context.isEmpty()) {
            return "未找到相关知识。请尝试其他关键词。";
        }
        
        return "根据知识库内容：\n\n" + context + "\n问题：" + question;
    }
    

    
    private String truncate(String text, int maxLength) {
        if (text == null || text.length() <= maxLength) {
            return text;
        }
        return text.substring(0, maxLength) + "...";
    }
    
    /**
     * 搜索结果数据类
     */
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class SearchResult {
        private UUID id;
        private String title;
        private String content;
        private String summary;
        private String category;
        private double score;
        private double fusedScore;
        private String matchType;
        private String highlight;
    }
}
