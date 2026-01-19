package io.github.samzhu.docmcp.repository;

import io.github.samzhu.docmcp.domain.model.Document;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * 文件資料存取介面
 * <p>
 * 提供文件的 CRUD 操作及全文搜尋功能。
 * </p>
 */
@Repository
public interface DocumentRepository extends CrudRepository<Document, UUID> {

    /**
     * 取得指定版本的所有文件
     *
     * @param versionId 版本 ID
     * @return 文件列表
     */
    @Query("SELECT * FROM documents WHERE version_id = :versionId")
    List<Document> findByVersionId(@Param("versionId") UUID versionId);

    /**
     * 根據版本 ID 和路徑查找文件
     *
     * @param versionId 版本 ID
     * @param path      文件路徑
     * @return 文件（若存在）
     */
    @Query("SELECT * FROM documents WHERE version_id = :versionId AND path = :path")
    Optional<Document> findByVersionIdAndPath(
            @Param("versionId") UUID versionId,
            @Param("path") String path
    );

    /**
     * 全文搜尋文件
     * <p>
     * 使用 PostgreSQL 的 tsvector 進行全文搜尋，
     * 標題權重較高（A），內容權重次之（B）。
     * </p>
     *
     * @param versionId 版本 ID
     * @param query     搜尋關鍵字
     * @param limit     最大回傳筆數
     * @return 符合條件的文件列表（依相關性排序）
     */
    @Query("""
            SELECT * FROM documents
            WHERE version_id = :versionId
            AND search_vector @@ plainto_tsquery('english', :query)
            ORDER BY ts_rank(search_vector, plainto_tsquery('english', :query)) DESC
            LIMIT :limit
            """)
    List<Document> fullTextSearch(
            @Param("versionId") UUID versionId,
            @Param("query") String query,
            @Param("limit") int limit
    );
}
