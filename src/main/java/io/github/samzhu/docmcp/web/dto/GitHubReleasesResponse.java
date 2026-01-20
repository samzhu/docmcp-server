package io.github.samzhu.docmcp.web.dto;

import java.util.List;

/**
 * GitHub Releases 回應物件
 * <p>
 * 包含預設的文件路徑和 Release 列表。
 * 前端可使用 defaultDocsPath 作為文件路徑的預設值。
 * </p>
 *
 * @param defaultDocsPath 預設文件路徑（從已知函式庫對應表取得，若無則為 "docs"）
 * @param releases        Release 列表
 */
public record GitHubReleasesResponse(
        String defaultDocsPath,
        List<GitHubReleaseDto> releases
) {
}
