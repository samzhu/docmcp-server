package io.github.samzhu.docmcp.infrastructure.github;

/**
 * GitHub 檔案資訊
 *
 * @param name        檔案名稱
 * @param path        檔案路徑
 * @param sha         SHA 值
 * @param size        檔案大小（bytes）
 * @param type        類型（file, dir）
 * @param downloadUrl 下載 URL
 */
public record GitHubFile(
        String name,
        String path,
        String sha,
        long size,
        String type,
        String downloadUrl
) {
    public boolean isDirectory() {
        return "dir".equals(type);
    }

    public boolean isFile() {
        return "file".equals(type);
    }
}
