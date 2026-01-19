package io.github.samzhu.docmcp.web.dto;

import io.github.samzhu.docmcp.domain.enums.SourceType;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * 更新函式庫請求
 * <p>
 * 用於更新函式庫的請求資料。
 * 所有欄位皆為可選，只更新有提供的欄位。
 * </p>
 *
 * @param displayName 顯示名稱
 * @param description 描述
 * @param sourceType  來源類型
 * @param sourceUrl   來源網址
 * @param category    分類
 * @param tags        標籤列表
 */
public record UpdateLibraryRequest(
        @Size(max = 200, message = "顯示名稱長度不可超過 200 字元")
        String displayName,

        @Size(max = 2000, message = "描述長度不可超過 2000 字元")
        String description,

        SourceType sourceType,

        @Size(max = 500, message = "來源網址長度不可超過 500 字元")
        String sourceUrl,

        @Size(max = 50, message = "分類長度不可超過 50 字元")
        String category,

        List<String> tags
) {}
