package io.github.samzhu.docmcp.service;

import io.github.samzhu.docmcp.domain.model.Document;
import io.github.samzhu.docmcp.mcp.dto.SearchResultItem;
import io.github.samzhu.docmcp.repository.DocumentRepository;
import io.github.samzhu.docmcp.repository.LibraryVersionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static io.github.samzhu.docmcp.infrastructure.vectorstore.DocumentChunkVectorStore.*;

/**
 * 搜尋服務
 * <p>
 * 提供全文檢索和語意搜尋功能。
 * 全文檢索使用 PostgreSQL 的 tsvector/tsquery。
 * 語意搜尋使用 pgvector 的向量相似度計算。
 * 混合搜尋使用 RRF（Reciprocal Rank Fusion）演算法融合兩種搜尋結果。
 * </p>
 */
@Service
@Transactional(readOnly = true)
public class SearchService {

    private static final Logger log = LoggerFactory.getLogger(SearchService.class);

    /**
     * RRF 常數 K，防止排名第一的結果權重過大
     * 參考：spring-documentation-mcp-server 使用 K=60
     */
    private static final int RRF_K = 60;

    private final DocumentRepository documentRepository;
    private final LibraryVersionRepository versionRepository;
    private final VectorStore vectorStore;

    /**
     * 混合搜尋的 alpha 參數，控制關鍵字搜尋與語意搜尋的權重比例
     * alpha = 關鍵字搜尋權重，(1 - alpha) = 語意搜尋權重
     * 預設 0.3 表示 30% 關鍵字，70% 語意
     */
    @Value("${docmcp.search.hybrid.alpha:0.3}")
    private double hybridAlpha;

    /**
     * 語意搜尋的最低相似度閾值
     */
    @Value("${docmcp.search.hybrid.min-similarity:0.5}")
    private double minSimilarity;

    public SearchService(DocumentRepository documentRepository,
                         LibraryVersionRepository versionRepository,
                         VectorStore vectorStore) {
        this.documentRepository = documentRepository;
        this.versionRepository = versionRepository;
        this.vectorStore = vectorStore;
    }

    /**
     * 全文檢索
     * <p>
     * 使用 PostgreSQL tsvector 進行全文搜尋，
     * 在文件標題和內容中搜尋關鍵字。
     * </p>
     *
     * @param libraryId 函式庫 ID（TSID 格式）
     * @param version   版本（可選，null 表示最新版本）
     * @param query     搜尋關鍵字
     * @param limit     結果數量上限
     * @return 搜尋結果列表
     */
    public List<SearchResultItem> fullTextSearch(String libraryId, String version,
                                                  String query, int limit) {
        if (query == null || query.isBlank()) {
            return List.of();
        }

        // 解析版本
        String versionId = resolveVersionId(libraryId, version);
        if (versionId == null) {
            return List.of();
        }

        // 執行全文搜尋
        List<Document> documents = documentRepository.fullTextSearch(versionId, query, limit);

        // 轉換為搜尋結果
        return documents.stream()
                .map(doc -> SearchResultItem.fromDocument(
                        doc.getId(),
                        doc.getTitle(),
                        doc.getPath(),
                        truncateContent(doc.getContent(), 500),
                        1.0  // 全文檢索不回傳分數，使用預設值
                ))
                .toList();
    }

    /**
     * 語意搜尋
     * <p>
     * 使用 VectorStore 進行向量相似度搜尋，
     * 將查詢文字轉換為向量後，搜尋相似的文件區塊。
     * </p>
     *
     * @param libraryId 函式庫 ID（TSID 格式）
     * @param version   版本（可選，null 表示最新版本）
     * @param query     自然語言查詢
     * @param limit     結果數量上限
     * @param threshold 相似度閾值（0-1，越高越嚴格）
     * @return 搜尋結果列表
     */
    public List<SearchResultItem> semanticSearch(String libraryId, String version,
                                                  String query, int limit, double threshold) {
        if (query == null || query.isBlank()) {
            return List.of();
        }

        // 解析版本
        String versionId = resolveVersionId(libraryId, version);
        if (versionId == null) {
            return List.of();
        }

        // 使用 VectorStore 執行語意搜尋
        // 透過 filterExpression 限制搜尋範圍為特定版本
        SearchRequest request = SearchRequest.builder()
                .query(query)
                .topK(limit)
                .similarityThreshold(threshold)
                .filterExpression(METADATA_VERSION_ID + " == '" + versionId + "'")
                .build();

        List<org.springframework.ai.document.Document> results = vectorStore.similaritySearch(request);

        if (results.isEmpty()) {
            return List.of();
        }

        // 從結果中取得 document IDs 並批次查詢文件資訊
        List<String> documentIds = results.stream()
                .map(doc -> {
                    Object docIdObj = doc.getMetadata().get(METADATA_DOCUMENT_ID);
                    return docIdObj != null ? docIdObj.toString() : null;
                })
                .filter(id -> id != null)
                .distinct()
                .toList();

        Map<String, Document> documentMap = StreamSupport.stream(
                        documentRepository.findAllById(documentIds).spliterator(), false)
                .collect(Collectors.toMap(Document::getId, Function.identity()));

        // 轉換為搜尋結果
        return results.stream()
                .map(doc -> toSearchResultItem(doc, documentMap))
                .filter(item -> item != null)
                .toList();
    }

    /**
     * 將 Spring AI Document 轉換為 SearchResultItem
     */
    private SearchResultItem toSearchResultItem(org.springframework.ai.document.Document doc,
                                                 Map<String, Document> documentMap) {
        Map<String, Object> metadata = doc.getMetadata();

        // 取得 document ID
        String documentId = getMetadataString(metadata, METADATA_DOCUMENT_ID);
        if (documentId == null) {
            return null;
        }

        // 取得對應的文件資訊
        Document dbDoc = documentMap.get(documentId);
        if (dbDoc == null) {
            return null;
        }

        // 取得 chunk ID 和其他資訊
        String chunkId = doc.getId();
        int chunkIndex = getMetadataInt(metadata, METADATA_CHUNK_INDEX, 0);
        double score = getMetadataDouble(metadata, "score", 0.0);

        return SearchResultItem.fromChunk(
                dbDoc.getId(),
                chunkId,
                dbDoc.getTitle(),
                dbDoc.getPath(),
                doc.getText(),
                score,
                chunkIndex
        );
    }

    /**
     * 從 metadata 取得字串值
     */
    private String getMetadataString(Map<String, Object> metadata, String key) {
        if (metadata == null || !metadata.containsKey(key)) {
            return null;
        }
        Object value = metadata.get(key);
        return value != null ? value.toString() : null;
    }

    /**
     * 從 metadata 取得整數值
     */
    private int getMetadataInt(Map<String, Object> metadata, String key, int defaultValue) {
        if (metadata == null || !metadata.containsKey(key)) {
            return defaultValue;
        }
        Object value = metadata.get(key);
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        try {
            return Integer.parseInt(value.toString());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    /**
     * 從 metadata 取得 double 值
     */
    private double getMetadataDouble(Map<String, Object> metadata, String key, double defaultValue) {
        if (metadata == null || !metadata.containsKey(key)) {
            return defaultValue;
        }
        Object value = metadata.get(key);
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        try {
            return Double.parseDouble(value.toString());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    /**
     * 解析版本 ID
     */
    private String resolveVersionId(String libraryId, String version) {
        if (version != null && !version.isBlank()) {
            return versionRepository.findByLibraryIdAndVersion(libraryId, version)
                    .map(v -> v.getId())
                    .orElse(null);
        } else {
            return versionRepository.findLatestByLibraryId(libraryId)
                    .map(v -> v.getId())
                    .orElse(null);
        }
    }

    /**
     * 截斷內容
     */
    private String truncateContent(String content, int maxLength) {
        if (content == null) {
            return "";
        }
        if (content.length() <= maxLength) {
            return content;
        }
        return content.substring(0, maxLength) + "...";
    }

    /**
     * 混合搜尋（使用 RRF 演算法融合全文搜尋與語意搜尋結果）
     * <p>
     * RRF（Reciprocal Rank Fusion）演算法：
     * 對每個搜尋結果計算 RRF 分數 = 1 / (K + rank)
     * 然後根據 alpha 參數加權融合兩種搜尋的 RRF 分數
     * final_score = alpha × keyword_rrf + (1 - alpha) × semantic_rrf
     * </p>
     *
     * @param libraryId 函式庫 ID（TSID 格式）
     * @param version   版本（可選，null 表示最新版本）
     * @param query     搜尋查詢
     * @param limit     結果數量上限
     * @return 融合後的搜尋結果列表（依 RRF 分數排序）
     */
    public List<SearchResultItem> hybridSearch(String libraryId, String version,
                                                String query, int limit) {
        return hybridSearch(libraryId, version, query, limit, hybridAlpha, minSimilarity);
    }

    /**
     * 混合搜尋（使用自訂參數）
     *
     * @param libraryId      函式庫 ID（TSID 格式）
     * @param version        版本（可選，null 表示最新版本）
     * @param query          搜尋查詢
     * @param limit          結果數量上限
     * @param alpha          關鍵字搜尋權重（0-1）
     * @param minSimilarity  語意搜尋最低相似度閾值
     * @return 融合後的搜尋結果列表（依 RRF 分數排序）
     */
    public List<SearchResultItem> hybridSearch(String libraryId, String version,
                                                String query, int limit,
                                                double alpha, double minSimilarity) {
        if (query == null || query.isBlank()) {
            return List.of();
        }

        log.debug("執行混合搜尋: query='{}', libraryId={}, alpha={}", query, libraryId, alpha);

        // 取得更多結果以確保融合後有足夠的資料
        int fetchLimit = limit * 2;

        // 並行執行兩種搜尋
        List<SearchResultItem> keywordResults = fullTextSearch(libraryId, version, query, fetchLimit);
        List<SearchResultItem> semanticResults = semanticSearch(libraryId, version, query, fetchLimit, minSimilarity);

        log.debug("關鍵字搜尋結果: {} 筆, 語意搜尋結果: {} 筆", keywordResults.size(), semanticResults.size());

        // 如果任一搜尋無結果，直接返回另一種搜尋的結果
        if (keywordResults.isEmpty() && semanticResults.isEmpty()) {
            return List.of();
        }
        if (keywordResults.isEmpty()) {
            return semanticResults.stream().limit(limit).toList();
        }
        if (semanticResults.isEmpty()) {
            return keywordResults.stream().limit(limit).toList();
        }

        // 計算 RRF 分數並融合
        Map<String, Double> rrfScores = calculateRRFScores(keywordResults, semanticResults, alpha);

        // 建立結果 ID 到 SearchResultItem 的對應（優先使用語意搜尋結果，因為有 chunk 資訊）
        Map<String, SearchResultItem> resultMap = new LinkedHashMap<>();
        for (SearchResultItem item : semanticResults) {
            resultMap.put(getResultKey(item), item);
        }
        for (SearchResultItem item : keywordResults) {
            resultMap.putIfAbsent(getResultKey(item), item);
        }

        // 依 RRF 分數排序並返回
        List<SearchResultItem> fusedResults = rrfScores.entrySet().stream()
                .filter(entry -> resultMap.containsKey(entry.getKey()))
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .limit(limit)
                .map(entry -> {
                    SearchResultItem original = resultMap.get(entry.getKey());
                    // 使用 RRF 分數替換原始分數（轉換為 0-1 範圍的正規化分數）
                    double normalizedScore = normalizeRRFScore(entry.getValue());
                    return original.withScore(normalizedScore);
                })
                .toList();

        log.debug("混合搜尋融合後結果: {} 筆", fusedResults.size());

        return fusedResults;
    }

    /**
     * 計算 RRF（倒數排名融合）分數
     * <p>
     * RRF 公式：score = Σ (1 / (K + rank))
     * 融合公式：final_score = alpha × keyword_rrf + (1 - alpha) × semantic_rrf
     * </p>
     *
     * @param keywordResults  關鍵字搜尋結果（已排序）
     * @param semanticResults 語意搜尋結果（已排序）
     * @param alpha           關鍵字搜尋權重
     * @return 結果 ID 到 RRF 分數的對應
     */
    private Map<String, Double> calculateRRFScores(List<SearchResultItem> keywordResults,
                                                    List<SearchResultItem> semanticResults,
                                                    double alpha) {
        Map<String, Double> scores = new HashMap<>();

        // 計算關鍵字搜尋的 RRF 分數
        for (int i = 0; i < keywordResults.size(); i++) {
            String key = getResultKey(keywordResults.get(i));
            double rrfScore = alpha * (1.0 / (RRF_K + i + 1));
            scores.merge(key, rrfScore, Double::sum);
        }

        // 計算語意搜尋的 RRF 分數
        for (int i = 0; i < semanticResults.size(); i++) {
            String key = getResultKey(semanticResults.get(i));
            double rrfScore = (1 - alpha) * (1.0 / (RRF_K + i + 1));
            scores.merge(key, rrfScore, Double::sum);
        }

        return scores;
    }

    /**
     * 取得搜尋結果的唯一識別鍵
     * 優先使用 chunkId（語意搜尋），若無則使用 documentId（全文搜尋）
     */
    private String getResultKey(SearchResultItem item) {
        if (item.chunkId() != null) {
            return "chunk:" + item.chunkId();
        }
        return "doc:" + item.documentId();
    }

    /**
     * 將 RRF 分數正規化為 0-1 範圍
     * RRF 分數最大值約為 2/(K+1)（當同一結果在兩種搜尋都排第一時）
     */
    private double normalizeRRFScore(double rrfScore) {
        double maxPossibleScore = 2.0 / (RRF_K + 1);
        return Math.min(1.0, rrfScore / maxPossibleScore);
    }
}
