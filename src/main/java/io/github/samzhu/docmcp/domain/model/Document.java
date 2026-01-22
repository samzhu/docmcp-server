package io.github.samzhu.docmcp.domain.model;

import jakarta.validation.constraints.Size;
import lombok.EqualsAndHashCode;
import lombok.Value;
import lombok.With;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.OffsetDateTime;
import java.util.Map;

/**
 * 文件實體
 * <p>
 * 儲存原始文件內容與元資料。每份文件屬於特定的函式庫版本，
 * 會被切割成多個 chunk 進行向量索引。
 * </p>
 * <p>
 * 使用 @Value 實現 Immutable Entity，@Version 進行樂觀鎖定。
 * version = null 表示新實體（執行 INSERT），version 有值表示既有實體（執行 UPDATE）。
 * </p>
 */
@Table("documents")
@Value
@EqualsAndHashCode(of = "id")
public class Document {

    /** 唯一識別碼（TSID 格式，13 字元） */
    @Id
    String id;

    /** 所屬版本 ID（TSID 格式） */
    @Column("version_id")
    String versionId;

    /** 文件標題 */
    @Size(max = 500)
    String title;

    /** 文件路徑 */
    @Size(max = 1000)
    String path;

    /** 文件內容 */
    @Size(max = 500000)
    String content;

    /** 內容雜湊值（用於偵測變更） */
    @Column("content_hash")
    @Size(max = 64)
    String contentHash;

    /** 文件類型（如 markdown、html） */
    @Column("doc_type")
    @Size(max = 50)
    String docType;

    /** 額外的元資料 */
    Map<String, Object> metadata;

    /** 樂觀鎖定版本號（null 表示新實體） */
    @Version
    @With
    Long version;

    /** 建立時間（由資料庫 DEFAULT 設定） */
    @Column("created_at")
    @With
    OffsetDateTime createdAt;

    /** 更新時間（由資料庫 DEFAULT 設定） */
    @Column("updated_at")
    @With
    OffsetDateTime updatedAt;

    /**
     * 建立新的文件
     * <p>
     * version = null 讓 Spring Data JDBC 判斷為新實體（執行 INSERT）
     * </p>
     *
     * @param id          應用層生成的 TSID
     * @param versionId   所屬版本 ID
     * @param title       文件標題
     * @param path        文件路徑
     * @param content     文件內容
     * @param contentHash 內容雜湊值
     * @param docType     文件類型
     * @return 新的文件實例
     */
    public static Document create(String id, String versionId, String title, String path,
                                   String content, String contentHash, String docType) {
        return new Document(id, versionId, title, path, content,
                contentHash, docType, Map.of(), null, null, null);
    }
}
