package io.github.samzhu.docmcp.domain.model;

import io.github.samzhu.docmcp.domain.enums.VersionStatus;
import lombok.EqualsAndHashCode;
import lombok.Value;
import lombok.With;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDate;
import java.time.OffsetDateTime;

/**
 * 函式庫版本實體
 * <p>
 * 每個函式庫可以有多個版本，文件會關聯到特定版本。
 * 支援標記最新版本、LTS 版本及版本狀態（ACTIVE、DEPRECATED、EOL）。
 * </p>
 * <p>
 * 使用 @Value 實現 Immutable Entity，@Version 進行樂觀鎖定。
 * entityVersion = null 表示新實體（執行 INSERT），entityVersion 有值表示既有實體（執行 UPDATE）。
 * </p>
 */
@Table("library_versions")
@Value
@EqualsAndHashCode(of = "id")
public class LibraryVersion {

    /** 唯一識別碼（TSID 格式，13 字元） */
    @Id
    String id;

    /** 所屬函式庫 ID（TSID 格式） */
    @Column("library_id")
    String libraryId;

    /** 版本號（如 3.2.0） */
    String version;

    /** 是否為最新版本 */
    @Column("is_latest")
    Boolean isLatest;

    /** 是否為 LTS（Long-Term Support）版本 */
    @Column("is_lts")
    Boolean isLts;

    /** 版本狀態 */
    VersionStatus status;

    /** 文件路徑 */
    @Column("docs_path")
    String docsPath;

    /** 發布日期 */
    @Column("release_date")
    LocalDate releaseDate;

    /** 樂觀鎖定版本號（null 表示新實體） */
    @Version
    @Column("entity_version")
    @With
    Long entityVersion;

    /** 建立時間（由資料庫 DEFAULT 設定） */
    @Column("created_at")
    @With
    OffsetDateTime createdAt;

    /** 更新時間（由資料庫 DEFAULT 設定） */
    @Column("updated_at")
    @With
    OffsetDateTime updatedAt;

    /**
     * 建立新的版本（使用預設值）
     * <p>
     * entityVersion = null 讓 Spring Data JDBC 判斷為新實體（執行 INSERT）
     * </p>
     *
     * @param id        應用層生成的 TSID
     * @param libraryId 所屬函式庫 ID
     * @param version   版本號
     * @param isLatest  是否為最新版本
     * @return 新的版本實例
     */
    public static LibraryVersion create(String id, String libraryId, String version, boolean isLatest) {
        return new LibraryVersion(id, libraryId, version, isLatest, false,
                VersionStatus.ACTIVE, null, null, null, null, null);
    }

    /**
     * 建立新的 LTS 版本
     * <p>
     * entityVersion = null 讓 Spring Data JDBC 判斷為新實體（執行 INSERT）
     * </p>
     *
     * @param id        應用層生成的 TSID
     * @param libraryId 所屬函式庫 ID
     * @param version   版本號
     * @param isLatest  是否為最新版本
     * @return 新的 LTS 版本實例
     */
    public static LibraryVersion createLts(String id, String libraryId, String version, boolean isLatest) {
        return new LibraryVersion(id, libraryId, version, isLatest, true,
                VersionStatus.ACTIVE, null, null, null, null, null);
    }
}
