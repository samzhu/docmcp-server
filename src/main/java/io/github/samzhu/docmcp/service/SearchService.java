package io.github.samzhu.docmcp.service;

import io.github.samzhu.docmcp.domain.model.Document;
import io.github.samzhu.docmcp.domain.model.DocumentChunk;
import io.github.samzhu.docmcp.mcp.dto.SearchResultItem;
import io.github.samzhu.docmcp.repository.DocumentChunkRepository;
import io.github.samzhu.docmcp.repository.DocumentRepository;
import io.github.samzhu.docmcp.repository.LibraryVersionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

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
    private final DocumentChunkRepository chunkRepository;
    private final LibraryVersionRepository versionRepository;
    private final EmbeddingService embeddingService;

    public SearchService(DocumentRepository documentRepository,
                         DocumentChunkRepository chunkRepository,
                         LibraryVersionRepository versionRepository,
                         EmbeddingService embeddingService) {
        this.documentRepository = documentRepository;
        this.chunkRepository = chunkRepository;
        this.versionRepository = versionRepository;
        this.embeddingService = embeddingService;
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
     * 使用向量相似度進行語意搜尋，
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

        // 將查詢轉換為向量
        float[] queryEmbedding = embeddingService.embed(query);
        String vectorString = embeddingService.toVectorString(queryEmbedding);

        // 執行向量相似度搜尋
        List<DocumentChunk> chunks = chunkRepository.findSimilarChunks(versionId, vectorString, limit);

        if (chunks.isEmpty()) {
            return List.of();
        }

        // 批次取得相關文件資訊
        List<UUID> documentIds = chunks.stream()
                .map(DocumentChunk::documentId)
                .distinct()
                .toList();

        Map<UUID, Document> documentMap = StreamSupport.stream(
                        documentRepository.findAllById(documentIds).spliterator(), false)
                .collect(Collectors.toMap(Document::id, Function.identity()));

        // 轉換為搜尋結果（包含相似度分數）
        List<SearchResultItem> results = new ArrayList<>();
        for (DocumentChunk chunk : chunks) {
            Document doc = documentMap.get(chunk.documentId());
            if (doc != null) {
                // 計算餘弦相似度（pgvector 回傳的是距離，需要轉換）
                double similarity = calculateCosineSimilarity(queryEmbedding, chunk.embedding());

                // 篩選超過閾值的結果
                if (similarity >= threshold) {
                    results.add(SearchResultItem.fromChunk(
                            doc.id(),
                            chunk.id(),
                            doc.title(),
                            doc.path(),
                            chunk.content(),
                            similarity,
                            chunk.chunkIndex()
                    ));
                }
            }
        }

        return results;
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

    /**
     * 計算餘弦相似度
     */
    private double calculateCosineSimilarity(float[] a, float[] b) {
        if (a == null || b == null || a.length != b.length) {
            return 0.0;
        }

        double dotProduct = 0.0;
        double normA = 0.0;
        double normB = 0.0;

        for (int i = 0; i < a.length; i++) {
            dotProduct += a[i] * b[i];
            normA += a[i] * a[i];
            normB += b[i] * b[i];
        }

        if (normA == 0 || normB == 0) {
            return 0.0;
        }

        return dotProduct / (Math.sqrt(normA) * Math.sqrt(normB));
    }
}
