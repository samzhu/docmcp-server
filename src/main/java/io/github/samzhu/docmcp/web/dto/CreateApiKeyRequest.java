package io.github.samzhu.docmcp.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.time.OffsetDateTime;

/**
 * 建立 API Key 請求
 *
 * @param name       識別名稱
 * @param expiresAt  過期時間（可選）
 * @param rateLimit  每小時請求上限（可選）
 */
public record CreateApiKeyRequest(
        @NotBlank(message = "名稱不可為空")
        @Size(max = 100, message = "名稱長度不可超過 100 字元")
        String name,

        OffsetDateTime expiresAt,

        @Positive(message = "速率限制必須為正數")
        Integer rateLimit
) {}
