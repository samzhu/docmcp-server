package io.github.samzhu.docmcp.mcp.dto;

import io.github.samzhu.docmcp.domain.model.LibraryVersion;

import java.time.LocalDate;

/**
 * 函式庫版本資料傳輸物件
 * <p>
 * 用於 MCP 工具回傳版本資訊。
 * </p>
 *
 * @param id          唯一識別碼
 * @param version     版本號
 * @param isLatest    是否為最新版本
 * @param isLts       是否為 LTS（Long-Term Support）版本
 * @param status      版本狀態 (ACTIVE, DEPRECATED, EOL)
 * @param docsPath    文件路徑
 * @param releaseDate 發布日期
 */
public record LibraryVersionDto(
        String id,
        String version,
        boolean isLatest,
        boolean isLts,
        String status,
        String docsPath,
        LocalDate releaseDate
) {
    /**
     * 從 LibraryVersion 實體轉換
     */
    public static LibraryVersionDto from(LibraryVersion libraryVersion) {
        return new LibraryVersionDto(
                libraryVersion.id() != null ? libraryVersion.id().toString() : null,
                libraryVersion.version(),
                libraryVersion.isLatest() != null && libraryVersion.isLatest(),
                libraryVersion.isLts() != null && libraryVersion.isLts(),
                libraryVersion.status() != null ? libraryVersion.status().name() : null,
                libraryVersion.docsPath(),
                libraryVersion.releaseDate()
        );
    }
}
