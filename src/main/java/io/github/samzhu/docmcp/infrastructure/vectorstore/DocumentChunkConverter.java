package io.github.samzhu.docmcp.infrastructure.vectorstore;

import io.github.samzhu.docmcp.domain.model.DocumentChunk;
import org.springframework.ai.document.Document;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static io.github.samzhu.docmcp.infrastructure.vectorstore.DocumentChunkVectorStore.*;

/**
 * DocumentChunk 與 Spring AI Document 轉換器
 * <p>
 * 負責在系統內部的 DocumentChunk 實體與 Spring AI 的 Document 之間進行轉換。
 * 這使得系統可以同時支援：
 * <ul>
 *   <li>內部使用 DocumentChunk 進行資料存取</li>
 *   <li>透過 VectorStore 介面與 Spring AI 生態系統整合</li>
 * </ul>
 * </p>
 * <p>
 * 注意：Spring AI 2.0 的 Document 不再儲存 embedding，
 * embedding 是由 VectorStore 內部管理的。
 * </p>
 */
@Component
public class DocumentChunkConverter {

    /**
     * 將 DocumentChunk 轉換為 Spring AI Document
     * <p>
     * 轉換時會將相關的元資料（versionId、documentId 等）放入 metadata 欄位，
     * 以支援 Spring AI 的 filter 機制。
     * </p>
     *
     * @param chunk     文件區塊實體
     * @param doc       所屬文件（用於取得標題和路徑）
     * @param versionId 版本 ID
     * @return Spring AI Document 物件
     */
    public Document toSpringAiDocument(
            DocumentChunk chunk,
            io.github.samzhu.docmcp.domain.model.Document doc,
            UUID versionId) {

        // 建構 metadata，包含所有相關資訊
        Map<String, Object> metadata = new HashMap<>();

        // 必要的索引欄位（用於 filter）
        metadata.put(METADATA_VERSION_ID, versionId.toString());
        metadata.put(METADATA_DOCUMENT_ID, chunk.documentId().toString());
        metadata.put(METADATA_CHUNK_INDEX, chunk.chunkIndex());

        // 可選的輔助欄位
        if (chunk.tokenCount() != null) {
            metadata.put(METADATA_TOKEN_COUNT, chunk.tokenCount());
        }

        // 從文件取得標題和路徑（如果有的話）
        if (doc != null) {
            if (doc.title() != null) {
                metadata.put(METADATA_DOCUMENT_TITLE, doc.title());
            }
            if (doc.path() != null) {
                metadata.put(METADATA_DOCUMENT_PATH, doc.path());
            }
        }

        // 合併原有的 metadata（如果有）
        if (chunk.metadata() != null && !chunk.metadata().isEmpty()) {
            for (Map.Entry<String, Object> entry : chunk.metadata().entrySet()) {
                // 不覆蓋上面設定的標準欄位
                metadata.putIfAbsent(entry.getKey(), entry.getValue());
            }
        }

        // Spring AI 2.0 的 Document 不再儲存 embedding
        return new Document(
                chunk.id() != null ? chunk.id().toString() : UUID.randomUUID().toString(),
                chunk.content(),
                metadata
        );
    }

    /**
     * 將 DocumentChunk 轉換為 Spring AI Document（簡化版本，不含 Document 資訊）
     *
     * @param chunk     文件區塊實體
     * @param versionId 版本 ID
     * @return Spring AI Document 物件
     */
    public Document toSpringAiDocument(DocumentChunk chunk, UUID versionId) {
        return toSpringAiDocument(chunk, null, versionId);
    }

    /**
     * 將 Spring AI Document 轉換為 DocumentChunk
     * <p>
     * 從 metadata 中解析所需的欄位（如 documentId、chunkIndex）。
     * 如果 metadata 中沒有這些欄位，會使用預設值。
     * </p>
     * <p>
     * 注意：Spring AI 2.0 的 Document 不包含 embedding，
     * 所以回傳的 DocumentChunk 的 embedding 欄位會是 null。
     * </p>
     *
     * @param doc Spring AI Document 物件
     * @return DocumentChunk 實體
     */
    public DocumentChunk fromSpringAiDocument(Document doc) {
        Map<String, Object> metadata = doc.getMetadata();

        // 從 metadata 解析 UUID 欄位
        UUID id = parseUuid(doc.getId(), null);
        UUID documentId = parseUuid(getMetadataString(metadata, METADATA_DOCUMENT_ID), null);

        // 從 metadata 解析其他欄位
        int chunkIndex = getMetadataInt(metadata, METADATA_CHUNK_INDEX, 0);
        int tokenCount = getMetadataInt(metadata, METADATA_TOKEN_COUNT, 0);

        // 建立 DocumentChunk 的 metadata（排除標準欄位）
        Map<String, Object> chunkMetadata = new HashMap<>();
        if (metadata != null) {
            for (Map.Entry<String, Object> entry : metadata.entrySet()) {
                String key = entry.getKey();
                // 排除已經提取到獨立欄位的 metadata
                if (!METADATA_VERSION_ID.equals(key) &&
                    !METADATA_DOCUMENT_ID.equals(key) &&
                    !METADATA_CHUNK_INDEX.equals(key) &&
                    !METADATA_TOKEN_COUNT.equals(key) &&
                    !METADATA_DOCUMENT_TITLE.equals(key) &&
                    !METADATA_DOCUMENT_PATH.equals(key) &&
                    !"score".equals(key)) {
                    chunkMetadata.put(key, entry.getValue());
                }
            }
        }

        // Spring AI 2.0 Document 不包含 embedding，所以設為 null
        return new DocumentChunk(
                id,
                documentId,
                chunkIndex,
                doc.getText(),
                null,  // embedding 由 VectorStore 管理
                tokenCount,
                chunkMetadata,
                null  // createdAt 由資料庫自動產生
        );
    }

    /**
     * 建立新文件區塊的 Spring AI Document
     * <p>
     * 用於同步時建立新的文件區塊。此方法會設定 ID。
     * </p>
     *
     * @param versionId  版本 ID
     * @param documentId 文件 ID
     * @param chunkIndex 區塊索引
     * @param content    區塊內容
     * @param tokenCount token 數量
     * @return Spring AI Document 物件（不含 embedding，由 VectorStore 自動生成）
     */
    public Document createNewChunkDocument(
            UUID versionId, UUID documentId, int chunkIndex, String content, int tokenCount) {

        return createNewChunkDocument(versionId, documentId, chunkIndex, content, tokenCount, null, null);
    }

    /**
     * 建立新文件區塊的 Spring AI Document（完整版本）
     *
     * @param versionId     版本 ID
     * @param documentId    文件 ID
     * @param chunkIndex    區塊索引
     * @param content       區塊內容
     * @param tokenCount    token 數量
     * @param documentTitle 文件標題（可選）
     * @param documentPath  文件路徑（可選）
     * @return Spring AI Document 物件
     */
    public Document createNewChunkDocument(
            UUID versionId, UUID documentId, int chunkIndex, String content, int tokenCount,
            String documentTitle, String documentPath) {

        Map<String, Object> metadata = new HashMap<>();
        metadata.put(METADATA_VERSION_ID, versionId.toString());
        metadata.put(METADATA_DOCUMENT_ID, documentId.toString());
        metadata.put(METADATA_CHUNK_INDEX, chunkIndex);
        metadata.put(METADATA_TOKEN_COUNT, tokenCount);

        if (documentTitle != null) {
            metadata.put(METADATA_DOCUMENT_TITLE, documentTitle);
        }
        if (documentPath != null) {
            metadata.put(METADATA_DOCUMENT_PATH, documentPath);
        }

        // Spring AI 2.0 使用 3 參數建構子
        return new Document(UUID.randomUUID().toString(), content, metadata);
    }

    // ========== 私有輔助方法 ==========

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
     * 解析 UUID 字串
     */
    private UUID parseUuid(String str, UUID defaultValue) {
        if (str == null || str.isBlank()) {
            return defaultValue;
        }
        try {
            return UUID.fromString(str);
        } catch (IllegalArgumentException e) {
            return defaultValue;
        }
    }
}
