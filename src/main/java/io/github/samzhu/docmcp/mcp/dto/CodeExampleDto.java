package io.github.samzhu.docmcp.mcp.dto;

import io.github.samzhu.docmcp.domain.model.CodeExample;

import java.util.UUID;

/**
 * 程式碼範例 DTO
 * <p>
 * 用於回傳程式碼範例資訊。
 * </p>
 *
 * @param id          範例 ID
 * @param documentId  所屬文件 ID
 * @param language    程式語言
 * @param code        程式碼內容
 * @param description 說明
 */
public record CodeExampleDto(
        UUID id,
        UUID documentId,
        String language,
        String code,
        String description
) {
    /**
     * 從 CodeExample 建立 DTO
     */
    public static CodeExampleDto from(CodeExample example) {
        return new CodeExampleDto(
                example.id(),
                example.documentId(),
                example.language(),
                example.code(),
                example.description()
        );
    }
}
