package io.github.samzhu.docmcp.mcp.dto;

import io.github.samzhu.docmcp.domain.model.Document;

import java.util.List;

/**
 * 文件內容 DTO
 * <p>
 * 用於回傳文件的完整內容資訊。
 * </p>
 *
 * @param id       文件 ID（TSID 格式）
 * @param title    文件標題
 * @param path     文件路徑
 * @param content  文件內容
 * @param docType  文件類型
 * @param chunks   文件區塊列表（可選）
 */
public record DocumentContentDto(
        String id,
        String title,
        String path,
        String content,
        String docType,
        List<ChunkDto> chunks
) {
    /**
     * 從 Document 建立 DTO
     */
    public static DocumentContentDto from(Document document, List<ChunkDto> chunks) {
        return new DocumentContentDto(
                document.getId(),
                document.getTitle(),
                document.getPath(),
                document.getContent(),
                document.getDocType(),
                chunks
        );
    }

    /**
     * 文件區塊 DTO
     *
     * @param id         區塊 ID（TSID 格式）
     * @param chunkIndex 區塊索引
     * @param content    區塊內容
     */
    public record ChunkDto(
            String id,
            int chunkIndex,
            String content
    ) {}
}
