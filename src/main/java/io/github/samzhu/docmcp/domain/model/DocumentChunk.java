package io.github.samzhu.docmcp.domain.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * 文件區塊實體
 * <p>
 * 文件被切割後的區塊，每個區塊會產生向量嵌入（embedding）
 * 用於語意搜尋。區塊大小通常為 500-1000 tokens。
 * </p>
 *
 * @param id         唯一識別碼
 * @param documentId 所屬文件 ID
 * @param chunkIndex 區塊索引（從 0 開始）
 * @param content    區塊內容
 * @param embedding  向量嵌入（768 維度，使用 gemini-embedding-001）
 * @param tokenCount token 數量
 * @param metadata   額外的元資料
 * @param createdAt  建立時間
 */
@Table("document_chunks")
public record DocumentChunk(
        @Id UUID id,
        @Column("document_id") UUID documentId,
        @Column("chunk_index") Integer chunkIndex,
        String content,
        float[] embedding,
        @Column("token_count") Integer tokenCount,
        Map<String, Object> metadata,
        @Column("created_at") OffsetDateTime createdAt
) {
    /**
     * 建立新的文件區塊
     */
    public static DocumentChunk create(UUID documentId, int chunkIndex,
                                        String content, float[] embedding, int tokenCount) {
        return new DocumentChunk(null, documentId, chunkIndex, content,
                embedding, tokenCount, Map.of(), null);
    }
}
