package io.github.samzhu.docmcp.domain.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * 文件實體
 * <p>
 * 儲存原始文件內容與元資料。每份文件屬於特定的函式庫版本，
 * 會被切割成多個 chunk 進行向量索引。
 * </p>
 *
 * @param id          唯一識別碼
 * @param versionId   所屬版本 ID
 * @param title       文件標題
 * @param path        文件路徑
 * @param content     文件內容
 * @param contentHash 內容雜湊值（用於偵測變更）
 * @param docType     文件類型（如 markdown、html）
 * @param metadata    額外的元資料
 * @param createdAt   建立時間
 * @param updatedAt   更新時間
 */
@Table("documents")
public record Document(
        @Id UUID id,
        @Column("version_id") UUID versionId,
        String title,
        String path,
        String content,
        @Column("content_hash") String contentHash,
        @Column("doc_type") String docType,
        Map<String, Object> metadata,
        @Column("created_at") OffsetDateTime createdAt,
        @Column("updated_at") OffsetDateTime updatedAt
) {
    /**
     * 建立新的文件
     */
    public static Document create(UUID versionId, String title, String path,
                                   String content, String contentHash, String docType) {
        return new Document(null, versionId, title, path, content,
                contentHash, docType, Map.of(), null, null);
    }
}
