package io.github.samzhu.docmcp.web.dto;

import io.github.samzhu.docmcp.domain.enums.SourceType;
import io.github.samzhu.docmcp.domain.model.Library;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Web API 用函式庫資料傳輸物件
 * <p>
 * 包含完整的函式庫資訊，用於 REST API 回應。
 * </p>
 *
 * @param id          唯一識別碼
 * @param name        函式庫名稱
 * @param displayName 顯示名稱
 * @param description 描述
 * @param sourceType  來源類型
 * @param sourceUrl   來源網址
 * @param category    分類
 * @param tags        標籤
 * @param createdAt   建立時間
 * @param updatedAt   更新時間
 */
public record WebLibraryDto(
        UUID id,
        String name,
        String displayName,
        String description,
        SourceType sourceType,
        String sourceUrl,
        String category,
        List<String> tags,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
    /**
     * 從 Library 實體轉換
     */
    public static WebLibraryDto from(Library library) {
        return new WebLibraryDto(
                library.id(),
                library.name(),
                library.displayName(),
                library.description(),
                library.sourceType(),
                library.sourceUrl(),
                library.category(),
                library.tags(),
                library.createdAt(),
                library.updatedAt()
        );
    }
}
