package io.github.samzhu.docmcp.service;

import io.github.samzhu.docmcp.domain.model.Document;
import io.github.samzhu.docmcp.mcp.dto.SearchResultItem;
import io.github.samzhu.docmcp.repository.DocumentRepository;
import io.github.samzhu.docmcp.repository.LibraryVersionRepository;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;
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
 * </p>
 */
@Service
@Transactional(readOnly = true)
public class SearchService {

    private final DocumentRepository documentRepository;
    private final LibraryVersionRepository versionRepository;
    private final VectorStore vectorStore;

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
     * @param libraryId 函式庫 ID
     * @param version   版本（可選，null 表示最新版本）
     * @param query     搜尋關鍵字
     * @param limit     結果數量上限
     * @return 搜尋結果列表
     */
    public List<SearchResultItem> fullTextSearch(UUID libraryId, String version,
                                                  String query, int limit) {
        if (query == null || query.isBlank()) {
            return List.of();
        }

        // 解析版本
        UUID versionId = resolveVersionId(libraryId, version);
        if (versionId == null) {
            return List.of();
        }

        // 執行全文搜尋
        List<Document> documents = documentRepository.fullTextSearch(versionId, query, limit);

        // 轉換為搜尋結果
        return documents.stream()
                .map(doc -> SearchResultItem.fromDocument(
                        doc.id(),
                        doc.title(),
                        doc.path(),
                        truncateContent(doc.content(), 500),
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
     * @param libraryId 函式庫 ID
     * @param version   版本（可選，null 表示最新版本）
     * @param query     自然語言查詢
     * @param limit     結果數量上限
     * @param threshold 相似度閾值（0-1，越高越嚴格）
     * @return 搜尋結果列表
     */
    public List<SearchResultItem> semanticSearch(UUID libraryId, String version,
                                                  String query, int limit, double threshold) {
        if (query == null || query.isBlank()) {
            return List.of();
        }

        // 解析版本
        UUID versionId = resolveVersionId(libraryId, version);
        if (versionId == null) {
            return List.of();
        }

        // 使用 VectorStore 執行語意搜尋
        // 透過 filterExpression 限制搜尋範圍為特定版本
        SearchRequest request = SearchRequest.builder()
                .query(query)
                .topK(limit)
                .similarityThreshold(threshold)
                .filterExpression(METADATA_VERSION_ID + " == '" + versionId.toString() + "'")
                .build();

        List<org.springframework.ai.document.Document> results = vectorStore.similaritySearch(request);

        if (results.isEmpty()) {
            return List.of();
        }

        // 從結果中取得 document IDs 並批次查詢文件資訊
        List<UUID> documentIds = results.stream()
                .map(doc -> {
                    Object docIdObj = doc.getMetadata().get(METADATA_DOCUMENT_ID);
                    return docIdObj != null ? parseUuid(docIdObj.toString()) : null;
                })
                .filter(id -> id != null)
                .distinct()
                .toList();

        Map<UUID, Document> documentMap = StreamSupport.stream(
                        documentRepository.findAllById(documentIds).spliterator(), false)
                .collect(Collectors.toMap(Document::id, Function.identity()));

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
                                                 Map<UUID, Document> documentMap) {
        Map<String, Object> metadata = doc.getMetadata();

        // 取得 document ID
        UUID documentId = parseUuid(getMetadataString(metadata, METADATA_DOCUMENT_ID));
        if (documentId == null) {
            return null;
        }

        // 取得對應的文件資訊
        Document dbDoc = documentMap.get(documentId);
        if (dbDoc == null) {
            return null;
        }

        // 取得 chunk ID 和其他資訊
        UUID chunkId = parseUuid(doc.getId());
        int chunkIndex = getMetadataInt(metadata, METADATA_CHUNK_INDEX, 0);
        double score = getMetadataDouble(metadata, "score", 0.0);

        return SearchResultItem.fromChunk(
                dbDoc.id(),
                chunkId,
                dbDoc.title(),
                dbDoc.path(),
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
     * 解析 UUID 字串
     */
    private UUID parseUuid(String str) {
        if (str == null || str.isBlank()) {
            return null;
        }
        try {
            return UUID.fromString(str);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /**
     * 解析版本 ID
     */
    private UUID resolveVersionId(UUID libraryId, String version) {
        if (version != null && !version.isBlank()) {
            return versionRepository.findByLibraryIdAndVersion(libraryId, version)
                    .map(v -> v.id())
                    .orElse(null);
        } else {
            return versionRepository.findLatestByLibraryId(libraryId)
                    .map(v -> v.id())
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
}
