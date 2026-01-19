package io.github.samzhu.docmcp.web.dto;

import io.github.samzhu.docmcp.domain.enums.SourceType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * 建立函式庫請求
 * <p>
 * 用於建立新函式庫的請求資料。
 * </p>
 *
 * @param name        函式庫名稱（唯一識別碼）
 * @param displayName 顯示名稱
 * @param description 描述
 * @param sourceType  來源類型
 * @param sourceUrl   來源網址（如 GitHub repo URL）
 * @param category    分類
 * @param tags        標籤列表
 */
public record CreateLibraryRequest(
        @NotBlank(message = "名稱不可為空")
        @Size(max = 100, message = "名稱長度不可超過 100 字元")
        @Pattern(regexp = "^[a-z0-9-]+$", message = "名稱只能包含小寫字母、數字和連字號")
        String name,

        @NotBlank(message = "顯示名稱不可為空")
        @Size(max = 200, message = "顯示名稱長度不可超過 200 字元")
        String displayName,

        @Size(max = 2000, message = "描述長度不可超過 2000 字元")
        String description,

        @NotNull(message = "來源類型不可為空")
        SourceType sourceType,

        @Size(max = 500, message = "來源網址長度不可超過 500 字元")
        String sourceUrl,

        @Size(max = 50, message = "分類長度不可超過 50 字元")
        String category,

        List<String> tags
) {}
