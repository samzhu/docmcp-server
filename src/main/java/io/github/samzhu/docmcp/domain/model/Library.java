package io.github.samzhu.docmcp.domain.model;

import io.github.samzhu.docmcp.domain.enums.SourceType;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * 函式庫實體
 * <p>
 * 代表一個可被索引的技術函式庫或框架，例如 Spring Boot、React 等。
 * 每個函式庫可以有多個版本，文件會關聯到特定版本。
 * </p>
 *
 * @param id          唯一識別碼
 * @param name        函式庫名稱（唯一，用於 API 查詢）
 * @param displayName 顯示名稱
 * @param description 函式庫描述
 * @param sourceType  來源類型（GITHUB、LOCAL、MANUAL）
 * @param sourceUrl   來源網址（如 GitHub repo URL）
 * @param category    分類（如 backend、frontend）
 * @param tags        標籤列表
 * @param createdAt   建立時間
 * @param updatedAt   更新時間
 */
@Table("libraries")
public record Library(
        @Id UUID id,
        String name,
        @Column("display_name") String displayName,
        String description,
        @Column("source_type") SourceType sourceType,
        @Column("source_url") String sourceUrl,
        String category,
        List<String> tags,
        @Column("created_at") OffsetDateTime createdAt,
        @Column("updated_at") OffsetDateTime updatedAt
) {
    /**
     * 建立新的函式庫（不含 ID 和時間戳記，由資料庫自動產生）
     */
    public static Library create(String name, String displayName, String description,
                                  SourceType sourceType, String sourceUrl,
                                  String category, List<String> tags) {
        return new Library(null, name, displayName, description, sourceType,
                sourceUrl, category, tags, null, null);
    }
}
