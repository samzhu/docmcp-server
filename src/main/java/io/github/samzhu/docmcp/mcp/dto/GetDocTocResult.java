package io.github.samzhu.docmcp.mcp.dto;

import java.util.List;

/**
 * get_doc_toc 工具的回傳結果
 *
 * @param libraryId   函式庫 ID
 * @param libraryName 函式庫名稱
 * @param version     版本號
 * @param entries     目錄條目列表
 * @param totalDocs   文件總數
 */
public record GetDocTocResult(
        String libraryId,
        String libraryName,
        String version,
        List<TocEntry> entries,
        int totalDocs
) {
    /**
     * 目錄條目
     *
     * @param documentId 文件 ID
     * @param title      文件標題
     * @param path       文件路徑
     * @param depth      層級深度 (0 為根層級)
     * @param children   子條目列表
     */
    public record TocEntry(
            String documentId,
            String title,
            String path,
            int depth,
            List<TocEntry> children
    ) {}
}
