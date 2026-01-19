package io.github.samzhu.docmcp.domain.model;

import io.github.samzhu.docmcp.domain.enums.VersionStatus;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * 函式庫版本實體
 * <p>
 * 每個函式庫可以有多個版本，文件會關聯到特定版本。
 * 支援標記最新版本及版本狀態（ACTIVE、DEPRECATED、EOL）。
 * </p>
 *
 * @param id          唯一識別碼
 * @param libraryId   所屬函式庫 ID
 * @param version     版本號（如 3.2.0）
 * @param isLatest    是否為最新版本
 * @param status      版本狀態
 * @param docsPath    文件路徑
 * @param releaseDate 發布日期
 * @param createdAt   建立時間
 * @param updatedAt   更新時間
 */
@Table("library_versions")
public record LibraryVersion(
        @Id UUID id,
        @Column("library_id") UUID libraryId,
        String version,
        @Column("is_latest") Boolean isLatest,
        VersionStatus status,
        @Column("docs_path") String docsPath,
        @Column("release_date") LocalDate releaseDate,
        @Column("created_at") OffsetDateTime createdAt,
        @Column("updated_at") OffsetDateTime updatedAt
) {
    /**
     * 建立新的版本（使用預設值）
     */
    public static LibraryVersion create(UUID libraryId, String version, boolean isLatest) {
        return new LibraryVersion(null, libraryId, version, isLatest,
                VersionStatus.ACTIVE, null, null, null, null);
    }
}
