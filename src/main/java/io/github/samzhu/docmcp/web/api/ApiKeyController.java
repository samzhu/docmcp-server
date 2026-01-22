package io.github.samzhu.docmcp.web.api;

import io.github.samzhu.docmcp.security.ApiKeyAuthenticationFilter;
import io.github.samzhu.docmcp.security.ApiKeyService;
import io.github.samzhu.docmcp.web.dto.ApiKeyDto;
import io.github.samzhu.docmcp.web.dto.CreateApiKeyRequest;
import io.github.samzhu.docmcp.web.dto.GeneratedApiKeyDto;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * API Key 管理 REST API
 * <p>
 * 提供 API Key 的建立、列表、撤銷功能。
 * 所有端點需要已認證的 API Key。
 * </p>
 */
@RestController
@RequestMapping("/api/keys")
public class ApiKeyController {

    private final ApiKeyService apiKeyService;

    public ApiKeyController(ApiKeyService apiKeyService) {
        this.apiKeyService = apiKeyService;
    }

    /**
     * 建立新的 API Key
     *
     * @param request   建立請求
     * @param principal 當前認證的 API Key
     * @return 包含原始金鑰的結果（只會顯示一次）
     */
    @PostMapping
    public ResponseEntity<GeneratedApiKeyDto> createKey(
            @RequestBody @Valid CreateApiKeyRequest request,
            @AuthenticationPrincipal ApiKeyAuthenticationFilter.ApiKeyPrincipal principal) {

        String createdBy = principal != null ? principal.name() : "system";

        var generated = apiKeyService.generateKey(
                request.name(),
                createdBy,
                request.expiresAt(),
                request.rateLimit()
        );

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(GeneratedApiKeyDto.from(generated));
    }

    /**
     * 列出所有 API Key
     *
     * @return API Key 列表（不含敏感資訊）
     */
    @GetMapping
    public List<ApiKeyDto> listKeys() {
        return apiKeyService.listKeys().stream()
                .map(ApiKeyDto::from)
                .toList();
    }

    /**
     * 取得單一 API Key
     *
     * @param id 金鑰 ID（TSID 格式）
     * @return API Key（不含敏感資訊）
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiKeyDto> getKey(@PathVariable String id) {
        return apiKeyService.getKeyById(id)
                .map(ApiKeyDto::from)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * 撤銷 API Key
     *
     * @param id 金鑰 ID（TSID 格式）
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> revokeKey(@PathVariable String id) {
        apiKeyService.revokeKey(id);
        return ResponseEntity.noContent().build();
    }
}
