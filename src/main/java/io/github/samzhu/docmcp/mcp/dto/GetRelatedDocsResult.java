package io.github.samzhu.docmcp.mcp.dto;

import java.util.List;

/**
 * get_related_docs 工具的回傳結果
 *
 * @param sourceDocumentId 來源文件 ID
 * @param sourceTitle      來源文件標題
 * @param sourcePath       來源文件路徑
 * @param relatedDocs      相關文件列表
 */
public record GetRelatedDocsResult(
        String sourceDocumentId,
        String sourceTitle,
        String sourcePath,
        List<RelatedDoc> relatedDocs
) {
    /**
     * 相關文件
     *
     * @param documentId 文件 ID
     * @param title      文件標題
     * @param path       文件路徑
     * @param similarity 相似度分數 (0-1)
     * @param snippet    內容片段
     */
    public record RelatedDoc(
            String documentId,
            String title,
            String path,
            double similarity,
            String snippet
    ) {}
}
