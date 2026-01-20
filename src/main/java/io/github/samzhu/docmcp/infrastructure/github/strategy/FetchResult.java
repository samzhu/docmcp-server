package io.github.samzhu.docmcp.infrastructure.github.strategy;

import io.github.samzhu.docmcp.infrastructure.github.GitHubFile;

import java.util.List;
import java.util.Map;

/**
 * GitHub 內容取得結果
 * <p>
 * 包含檔案列表、預載入的內容（Archive 策略會預先載入）、
 * 以及使用的策略名稱。
 * </p>
 *
 * @param files        檔案列表
 * @param contents     檔案路徑 → 內容（Archive 策略會預先載入，其他策略為空）
 * @param strategyUsed 使用的策略名稱
 */
public record FetchResult(
        List<GitHubFile> files,
        Map<String, String> contents,
        String strategyUsed
) {
    /**
     * 建立只有檔案列表的結果（無預載入內容）
     */
    public static FetchResult of(List<GitHubFile> files, String strategyUsed) {
        return new FetchResult(files, Map.of(), strategyUsed);
    }

    /**
     * 建立包含預載入內容的結果
     */
    public static FetchResult withContents(List<GitHubFile> files, Map<String, String> contents, String strategyUsed) {
        return new FetchResult(files, contents, strategyUsed);
    }

    /**
     * 檢查指定檔案是否有預載入內容
     */
    public boolean hasContent(String path) {
        return contents.containsKey(path);
    }

    /**
     * 取得預載入的檔案內容
     */
    public String getContent(String path) {
        return contents.get(path);
    }
}
