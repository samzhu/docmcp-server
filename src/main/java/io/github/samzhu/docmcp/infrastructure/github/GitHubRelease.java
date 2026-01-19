package io.github.samzhu.docmcp.infrastructure.github;

import java.time.OffsetDateTime;

/**
 * GitHub Release 資訊
 *
 * @param id          Release ID
 * @param tagName     標籤名稱（版本號）
 * @param name        Release 名稱
 * @param body        Release 內容描述
 * @param draft       是否為草稿
 * @param prerelease  是否為預發行版本
 * @param publishedAt 發布時間
 * @param tarballUrl  tarball 下載 URL
 * @param zipballUrl  zipball 下載 URL
 */
public record GitHubRelease(
        long id,
        String tagName,
        String name,
        String body,
        boolean draft,
        boolean prerelease,
        OffsetDateTime publishedAt,
        String tarballUrl,
        String zipballUrl
) {}
