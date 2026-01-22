package io.github.samzhu.docmcp.web.dto;

import io.github.samzhu.docmcp.domain.enums.VersionStatus;
import io.github.samzhu.docmcp.domain.model.LibraryVersion;

import java.time.LocalDate;
import java.time.OffsetDateTime;

/**
 * 函式庫版本資料傳輸物件
 * <p>
 * 用於 Web API 回傳版本資訊。
 * </p>
 *
 * @param id          版本 ID（TSID 格式）
 * @param libraryId   函式庫 ID（TSID 格式）
 * @param version     版本號
 * @param isLatest    是否為最新版本
 * @param status      版本狀態
 * @param docsPath    文件路徑
 * @param releaseDate 發布日期
 * @param createdAt   建立時間
 * @param updatedAt   更新時間
 */
public record LibraryVersionDto(
        String id,
        String libraryId,
        String version,
        Boolean isLatest,
        VersionStatus status,
        String docsPath,
        LocalDate releaseDate,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
    /**
     * 從 LibraryVersion 實體轉換
     */
    public static LibraryVersionDto from(LibraryVersion version) {
        return new LibraryVersionDto(
                version.getId(),
                version.getLibraryId(),
                version.getVersion(),
                version.getIsLatest(),
                version.getStatus(),
                version.getDocsPath(),
                version.getReleaseDate(),
                version.getCreatedAt(),
                version.getUpdatedAt()
        );
    }
}
