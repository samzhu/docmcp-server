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
 * 程式碼範例實體
 * <p>
 * 從文件中萃取的程式碼範例，包含語言類型、程式碼內容及說明。
 * 可用於快速查找特定功能的使用範例。
 * </p>
 * <p>
 * 使用 @Value 實現 Immutable Entity，@Version 進行樂觀鎖定。
 * version = null 表示新實體（執行 INSERT），version 有值表示既有實體（執行 UPDATE）。
 * </p>
 */
@Table("code_examples")
@Value
@EqualsAndHashCode(of = "id")
@ToString(exclude = "code")
public class CodeExample {

    /** 唯一識別碼（TSID 格式，13 字元） */
    @Id
    String id;

    /** 所屬文件 ID（TSID 格式） */
    @Column("document_id")
    String documentId;

    /** 程式語言（如 java、javascript） */
    @Size(max = 50)
    String language;

    /** 程式碼內容 */
    @Size(max = 50000)
    String code;

    /** 程式碼說明 */
    @Size(max = 1000)
    String description;

    /** 起始行號 */
    @Column("start_line")
    Integer startLine;

    /** 結束行號 */
    @Column("end_line")
    Integer endLine;

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
     * 建立新的程式碼範例
     * <p>
     * version = null 讓 Spring Data JDBC 判斷為新實體（執行 INSERT）
     * </p>
     *
     * @param id          應用層生成的 TSID
     * @param documentId  所屬文件 ID
     * @param language    程式語言
     * @param code        程式碼內容
     * @param description 程式碼說明
     * @return 新的程式碼範例實例
     */
    public static CodeExample create(String id, String documentId, String language,
                                      String code, String description) {
        return new CodeExample(id, documentId, language, code,
                description, null, null, Map.of(), null, null, null);
    }
}
