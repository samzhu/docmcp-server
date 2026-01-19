package io.github.samzhu.docmcp.security;

import io.github.samzhu.docmcp.domain.enums.ApiKeyStatus;
import io.github.samzhu.docmcp.domain.model.ApiKey;
import io.github.samzhu.docmcp.repository.ApiKeyRepository;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * API Key 服務
 * <p>
 * 提供 API Key 的生成、驗證、撤銷等功能。
 * </p>
 */
@Service
@Transactional(readOnly = true)
public class ApiKeyService {

    private static final String KEY_PREFIX = "dmcp_";
    private static final int KEY_LENGTH = 32;
    private static final int DEFAULT_RATE_LIMIT = 1000;

    private final ApiKeyRepository apiKeyRepository;
    private final PasswordEncoder passwordEncoder;
    private final SecureRandom secureRandom;

    public ApiKeyService(ApiKeyRepository apiKeyRepository) {
        this.apiKeyRepository = apiKeyRepository;
        this.passwordEncoder = new BCryptPasswordEncoder();
        this.secureRandom = new SecureRandom();
    }

    /**
     * 產生新的 API Key
     *
     * @param name      識別名稱
     * @param createdBy 建立者
     * @return 包含原始金鑰的結果（只會顯示一次）
     */
    @Transactional
    public GeneratedApiKey generateKey(String name, String createdBy) {
        return generateKey(name, createdBy, null, DEFAULT_RATE_LIMIT);
    }

    /**
     * 產生新的 API Key（含過期時間和速率限制）
     *
     * @param name      識別名稱
     * @param createdBy 建立者
     * @param expiresAt 過期時間（null 表示永不過期）
     * @param rateLimit 每小時請求上限
     * @return 包含原始金鑰的結果（只會顯示一次）
     */
    @Transactional
    public GeneratedApiKey generateKey(String name, String createdBy,
                                        OffsetDateTime expiresAt, Integer rateLimit) {
        // 檢查名稱是否已存在
        if (apiKeyRepository.findByName(name).isPresent()) {
            throw new IllegalArgumentException("API Key 名稱已存在: " + name);
        }

        // 生成隨機金鑰
        String rawKey = generateRawKey();
        String keyPrefix = rawKey.substring(0, 12);  // dmcp_xxxxxxxx
        String keyHash = passwordEncoder.encode(rawKey);

        // 儲存金鑰
        ApiKey apiKey = ApiKey.create(name, keyHash, keyPrefix,
                rateLimit != null ? rateLimit : DEFAULT_RATE_LIMIT,
                expiresAt, createdBy);
        apiKey = apiKeyRepository.save(apiKey);

        return new GeneratedApiKey(apiKey.id(), name, rawKey, keyPrefix);
    }

    /**
     * 驗證 API Key
     *
     * @param rawKey 原始金鑰
     * @return 驗證通過的 API Key（若有效）
     */
    public Optional<ApiKey> validateKey(String rawKey) {
        if (rawKey == null || !rawKey.startsWith(KEY_PREFIX)) {
            return Optional.empty();
        }

        // 從前綴查找金鑰
        String keyPrefix = rawKey.length() >= 12 ? rawKey.substring(0, 12) : rawKey;
        Optional<ApiKey> apiKeyOpt = apiKeyRepository.findByKeyPrefix(keyPrefix);

        if (apiKeyOpt.isEmpty()) {
            return Optional.empty();
        }

        ApiKey apiKey = apiKeyOpt.get();

        // 檢查狀態和過期
        if (!apiKey.isValid()) {
            return Optional.empty();
        }

        // 驗證雜湊
        if (!passwordEncoder.matches(rawKey, apiKey.keyHash())) {
            return Optional.empty();
        }

        return Optional.of(apiKey);
    }

    /**
     * 更新最後使用時間
     *
     * @param apiKey 要更新的 API Key
     */
    @Transactional
    public void updateLastUsed(ApiKey apiKey) {
        ApiKey updated = apiKey.withLastUsedAt(OffsetDateTime.now());
        apiKeyRepository.save(updated);
    }

    /**
     * 撤銷 API Key
     *
     * @param keyId 金鑰 ID
     */
    @Transactional
    public void revokeKey(UUID keyId) {
        ApiKey apiKey = apiKeyRepository.findById(keyId)
                .orElseThrow(() -> new IllegalArgumentException("找不到 API Key: " + keyId));

        apiKeyRepository.save(apiKey.revoke());
    }

    /**
     * 取得所有 API Key
     *
     * @return API Key 列表
     */
    public List<ApiKey> listKeys() {
        return apiKeyRepository.findAllOrderByCreatedAtDesc();
    }

    /**
     * 根據 ID 取得 API Key
     *
     * @param id 金鑰 ID
     * @return API Key（若存在）
     */
    public Optional<ApiKey> getKeyById(UUID id) {
        return apiKeyRepository.findById(id);
    }

    /**
     * 取得有效的 API Key
     *
     * @return 有效的 API Key 列表
     */
    public List<ApiKey> listActiveKeys() {
        return apiKeyRepository.findByStatus(ApiKeyStatus.ACTIVE);
    }

    /**
     * 生成隨機金鑰
     */
    private String generateRawKey() {
        byte[] bytes = new byte[KEY_LENGTH];
        secureRandom.nextBytes(bytes);
        String randomPart = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        return KEY_PREFIX + randomPart.substring(0, KEY_LENGTH);
    }

    /**
     * 生成的 API Key 結果
     *
     * @param id        金鑰 ID
     * @param name      名稱
     * @param rawKey    原始金鑰（只會顯示一次）
     * @param keyPrefix 金鑰前綴
     */
    public record GeneratedApiKey(
            UUID id,
            String name,
            String rawKey,
            String keyPrefix
    ) {}
}
