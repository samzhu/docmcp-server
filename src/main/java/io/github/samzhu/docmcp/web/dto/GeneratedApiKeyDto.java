package io.github.samzhu.docmcp.web.dto;

import io.github.samzhu.docmcp.security.ApiKeyService;

import java.util.UUID;

/**
 * 生成的 API Key 結果 DTO
 * <p>
 * 包含原始金鑰，只會在建立時顯示一次。
 * </p>
 *
 * @param id        金鑰 ID
 * @param name      識別名稱
 * @param rawKey    原始金鑰（只會顯示一次，請妥善保存）
 * @param keyPrefix 金鑰前綴
 */
public record GeneratedApiKeyDto(
        UUID id,
        String name,
        String rawKey,
        String keyPrefix
) {
    /**
     * 從 GeneratedApiKey 轉換
     */
    public static GeneratedApiKeyDto from(ApiKeyService.GeneratedApiKey generated) {
        return new GeneratedApiKeyDto(
                generated.id(),
                generated.name(),
                generated.rawKey(),
                generated.keyPrefix()
        );
    }
}
