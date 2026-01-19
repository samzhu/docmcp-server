package io.github.samzhu.docmcp.domain.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * 程式碼範例實體
 * <p>
 * 從文件中萃取的程式碼範例，包含語言類型、程式碼內容及說明。
 * 可用於快速查找特定功能的使用範例。
 * </p>
 *
 * @param id          唯一識別碼
 * @param documentId  所屬文件 ID
 * @param language    程式語言（如 java、javascript）
 * @param code        程式碼內容
 * @param description 程式碼說明
 * @param startLine   起始行號
 * @param endLine     結束行號
 * @param metadata    額外的元資料
 * @param createdAt   建立時間
 */
@Table("code_examples")
public record CodeExample(
        @Id UUID id,
        @Column("document_id") UUID documentId,
        String language,
        String code,
        String description,
        @Column("start_line") Integer startLine,
        @Column("end_line") Integer endLine,
        Map<String, Object> metadata,
        @Column("created_at") OffsetDateTime createdAt
) {
    /**
     * 建立新的程式碼範例
     */
    public static CodeExample create(UUID documentId, String language,
                                      String code, String description) {
        return new CodeExample(null, documentId, language, code,
                description, null, null, Map.of(), null);
    }
}
