package io.github.samzhu.docmcp.domain.model;

import jakarta.validation.constraints.Size;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.Value;
import lombok.With;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.OffsetDateTime;
import java.util.Map;

/**
 * 文件區塊實體
 * <p>
 * 文件被切割後的區塊，每個區塊會產生向量嵌入（embedding）
 * 用於語意搜尋。區塊大小通常為 500-1000 tokens。
 * </p>
 * <p>
 * 使用 @Value 實現 Immutable Entity，@Version 進行樂觀鎖定。
 * version = null 表示新實體（執行 INSERT），version 有值表示既有實體（執行 UPDATE）。
 * </p>
 */
@Table("document_chunks")
@Value
@EqualsAndHashCode(of = "id")
@ToString(exclude = "embedding")
public class DocumentChunk {

    /** 唯一識別碼（TSID 格式，13 字元） */
    @Id
    String id;

    /** 所屬文件 ID（TSID 格式） */
    @Column("document_id")
    String documentId;

    /** 區塊索引（從 0 開始） */
    @Column("chunk_index")
    Integer chunkIndex;

    /** 區塊內容 */
    @Size(max = 10000)
    String content;

    /** 向量嵌入（768 維度，使用 gemini-embedding-001） */
    float[] embedding;

    /** token 數量 */
    @Column("token_count")
    Integer tokenCount;

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
     * 建立新的文件區塊
     * <p>
     * version = null 讓 Spring Data JDBC 判斷為新實體（執行 INSERT）
     * </p>
     *
     * @param id         應用層生成的 TSID
     * @param documentId 所屬文件 ID
     * @param chunkIndex 區塊索引
     * @param content    區塊內容
     * @param embedding  向量嵌入
     * @param tokenCount token 數量
     * @return 新的文件區塊實例
     */
    public static DocumentChunk create(String id, String documentId, int chunkIndex,
                                        String content, float[] embedding, int tokenCount) {
        return new DocumentChunk(id, documentId, chunkIndex, content,
                embedding, tokenCount, Map.of(), null, null, null);
    }
}
