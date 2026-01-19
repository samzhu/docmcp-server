package io.github.samzhu.docmcp.mcp.dto;

import io.github.samzhu.docmcp.domain.model.Document;

import java.util.List;
import java.util.UUID;

/**
 * 文件內容 DTO
 * <p>
 * 用於回傳文件的完整內容資訊。
 * </p>
 *
 * @param id       文件 ID
 * @param title    文件標題
 * @param path     文件路徑
 * @param content  文件內容
 * @param docType  文件類型
 * @param chunks   文件區塊列表（可選）
 */
public record DocumentContentDto(
        UUID id,
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
                document.id(),
                document.title(),
                document.path(),
                document.content(),
                document.docType(),
                chunks
        );
    }

    /**
     * 文件區塊 DTO
     */
    public record ChunkDto(
            UUID id,
            int chunkIndex,
            String content
    ) {}
}
