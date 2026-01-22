package io.github.samzhu.docmcp.domain.model;

import io.github.samzhu.docmcp.domain.enums.SourceType;
import jakarta.validation.constraints.Size;
import lombok.EqualsAndHashCode;
import lombok.Value;
import lombok.With;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * 函式庫實體
 * <p>
 * 代表一個可被索引的技術函式庫或框架，例如 Spring Boot、React 等。
 * 每個函式庫可以有多個版本，文件會關聯到特定版本。
 * </p>
 * <p>
 * 使用 @Value 實現 Immutable Entity，@Version 進行樂觀鎖定。
 * version = null 表示新實體（執行 INSERT），version 有值表示既有實體（執行 UPDATE）。
 * </p>
 */
@Table("libraries")
@Value
@EqualsAndHashCode(of = "id")
public class Library {

    /** 唯一識別碼（TSID 格式，13 字元） */
    @Id
    String id;

    /** 函式庫名稱（唯一，用於 API 查詢） */
    @Size(max = 100)
    String name;

    /** 顯示名稱 */
    @Column("display_name")
    @Size(max = 200)
    String displayName;

    /** 函式庫描述 */
    @Size(max = 2000)
    String description;

    /** 來源類型（GITHUB、LOCAL、MANUAL） */
    @Column("source_type")
    SourceType sourceType;

    /** 來源網址（如 GitHub repo URL） */
    @Column("source_url")
    @Size(max = 500)
    String sourceUrl;

    /** 分類（如 backend、frontend） */
    @Size(max = 50)
    String category;

    /** 標籤列表 */
    List<String> tags;

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
     * 建立新的函式庫（需傳入應用層生成的 TSID）
     * <p>
     * version = null 讓 Spring Data JDBC 判斷為新實體（執行 INSERT）
     * </p>
     *
     * @param id          應用層生成的 TSID
     * @param name        函式庫名稱
     * @param displayName 顯示名稱
     * @param description 函式庫描述
     * @param sourceType  來源類型
     * @param sourceUrl   來源網址
     * @param category    分類
     * @param tags        標籤列表
     * @return 新的函式庫實例
     */
    public static Library create(String id, String name, String displayName, String description,
                                  SourceType sourceType, String sourceUrl,
                                  String category, List<String> tags) {
        return new Library(id, name, displayName, description, sourceType,
                sourceUrl, category, tags, null, null, null);
    }
}
