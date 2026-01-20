package io.github.samzhu.docmcp.web.dto;

import io.github.samzhu.docmcp.infrastructure.github.GitHubRelease;

import java.time.OffsetDateTime;

/**
 * GitHub Release 資料傳輸物件
 * <p>
 * 用於 Web API 回傳 GitHub Release 資訊，包含該版本是否已存在於系統中的標記，
 * 以及根據版本號計算出的文件路徑。
 * </p>
 *
 * @param tagName     標籤名稱（原始值，如 v4.0.1）
 * @param version     正規化版本號（移除 v 前綴，如 4.0.1）
 * @param name        Release 名稱
 * @param publishedAt 發布時間
 * @param exists      是否已存在於系統中
 * @param docsPath    該版本對應的文件路徑（根據版本號計算）
 */
public record GitHubReleaseDto(
        String tagName,
        String version,
        String name,
        OffsetDateTime publishedAt,
        boolean exists,
        String docsPath
) {
    /**
     * 從 GitHubRelease 轉換，並標記是否已存在及文件路徑
     *
     * @param release  GitHub Release 資訊
     * @param exists   是否已存在於系統中
     * @param docsPath 該版本對應的文件路徑
     * @return GitHubReleaseDto
     */
    public static GitHubReleaseDto from(GitHubRelease release, boolean exists, String docsPath) {
        return new GitHubReleaseDto(
                release.tagName(),
                normalizeVersion(release.tagName()),
                getNameWithFallback(release.name(), release.tagName()),
                release.publishedAt(),
                exists,
                docsPath
        );
    }

    /**
     * 取得 Release 名稱，若為空則使用 tagName 作為 fallback
     * <p>
     * GitHub 有時候 Release 只有 tag 沒有額外設定 name，此時 name 會是 null 或空字串。
     * 為了前端顯示友好，使用 tagName 作為 fallback。
     * </p>
     *
     * @param name    Release 名稱（可能為 null 或空字串）
     * @param tagName 標籤名稱（作為 fallback）
     * @return 有效的名稱
     */
    private static String getNameWithFallback(String name, String tagName) {
        if (name == null || name.isBlank()) {
            return tagName;
        }
        return name;
    }

    /**
     * 正規化版本號（移除 v 或 V 前綴）
     *
     * @param tagName 標籤名稱
     * @return 正規化後的版本號
     */
    private static String normalizeVersion(String tagName) {
        if (tagName == null) {
            return null;
        }
        // 移除 v 或 V 前綴
        if (tagName.startsWith("v") || tagName.startsWith("V")) {
            return tagName.substring(1);
        }
        return tagName;
    }
}
