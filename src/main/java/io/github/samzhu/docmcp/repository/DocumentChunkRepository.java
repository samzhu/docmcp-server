package io.github.samzhu.docmcp.repository;

import io.github.samzhu.docmcp.domain.model.DocumentChunk;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * 文件區塊資料存取介面
 * <p>
 * 提供文件區塊的 CRUD 操作及向量相似度搜尋功能。
 * </p>
 */
@Repository
public interface DocumentChunkRepository extends CrudRepository<DocumentChunk, UUID> {

    /**
     * 取得指定文件的所有區塊（依索引排序）
     *
     * @param documentId 文件 ID
     * @return 區塊列表
     */
    @Query("SELECT * FROM document_chunks WHERE document_id = :documentId ORDER BY chunk_index")
    List<DocumentChunk> findByDocumentIdOrderByChunkIndex(@Param("documentId") UUID documentId);

    /**
     * 向量相似度搜尋
     * <p>
     * 使用 pgvector 的餘弦距離進行相似度搜尋，
     * 回傳最相似的區塊列表。
     * </p>
     *
     * @param versionId      版本 ID
     * @param queryEmbedding 查詢向量（字串格式，如 "[0.1, 0.2, ...]"）
     * @param limit          最大回傳筆數
     * @return 最相似的區塊列表
     */
    @Query("""
            SELECT dc.* FROM document_chunks dc
            JOIN documents d ON dc.document_id = d.id
            WHERE d.version_id = :versionId
            AND dc.embedding IS NOT NULL
            ORDER BY dc.embedding <=> cast(:queryEmbedding as vector)
            LIMIT :limit
            """)
    List<DocumentChunk> findSimilarChunks(
            @Param("versionId") UUID versionId,
            @Param("queryEmbedding") String queryEmbedding,
            @Param("limit") int limit
    );
}
