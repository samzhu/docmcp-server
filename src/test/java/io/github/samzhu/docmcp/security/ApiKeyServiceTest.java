package io.github.samzhu.docmcp.security;

import com.github.f4b6a3.tsid.TsidCreator;
import io.github.samzhu.docmcp.domain.enums.ApiKeyStatus;
import io.github.samzhu.docmcp.domain.model.ApiKey;
import io.github.samzhu.docmcp.repository.ApiKeyRepository;
import io.github.samzhu.docmcp.service.IdService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * ApiKeyService 單元測試
 */
@ExtendWith(MockitoExtension.class)
class ApiKeyServiceTest {

    @Mock
    private IdService idService;

    @Mock
    private ApiKeyRepository apiKeyRepository;

    private ApiKeyService apiKeyService;

    /**
     * 產生隨機 ID
     */
    private String randomId() {
        return TsidCreator.getTsid().toString();
    }

    @BeforeEach
    void setUp() {
        apiKeyService = new ApiKeyService(idService, apiKeyRepository);
    }

    @Test
    void shouldGenerateNewApiKey() {
        // 準備
        when(apiKeyRepository.findByName("test-key")).thenReturn(Optional.empty());
        when(apiKeyRepository.save(any(ApiKey.class))).thenAnswer(invocation -> {
            ApiKey apiKey = invocation.getArgument(0);
            var now = OffsetDateTime.now();
            return new ApiKey(
                    randomId(),
                    apiKey.getName(),
                    apiKey.getKeyHash(),
                    apiKey.getKeyPrefix(),
                    apiKey.getStatus(),
                    apiKey.getRateLimit(),
                    apiKey.getExpiresAt(),
                    apiKey.getLastUsedAt(),
                    apiKey.getCreatedBy(),
                    0L,         // version（模擬從資料庫讀取）
                    now,        // createdAt
                    now         // updatedAt
            );
        });

        // 執行
        var result = apiKeyService.generateKey("test-key", "admin");

        // 驗證
        assertThat(result.name()).isEqualTo("test-key");
        assertThat(result.rawKey()).startsWith("dmcp_");
        assertThat(result.keyPrefix()).startsWith("dmcp_");
        assertThat(result.id()).isNotNull();

        // 驗證儲存的資料
        ArgumentCaptor<ApiKey> captor = ArgumentCaptor.forClass(ApiKey.class);
        verify(apiKeyRepository).save(captor.capture());

        ApiKey savedKey = captor.getValue();
        assertThat(savedKey.getName()).isEqualTo("test-key");
        assertThat(savedKey.getStatus()).isEqualTo(ApiKeyStatus.ACTIVE);
        assertThat(savedKey.getCreatedBy()).isEqualTo("admin");
    }

    @Test
    void shouldThrowExceptionWhenNameAlreadyExists() {
        // 準備
        var existingKey = createApiKey("existing-key", "dmcp_test1234");
        when(apiKeyRepository.findByName("existing-key")).thenReturn(Optional.of(existingKey));

        // 執行 & 驗證
        assertThatThrownBy(() -> apiKeyService.generateKey("existing-key", "admin"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("existing-key");
    }

    @Test
    void shouldValidateApiKey() {
        // 準備
        String rawKey = "dmcp_test123456789012345678901234";
        String keyPrefix = "dmcp_test123";  // 12 characters

        // 建立一個使用 BCrypt 雜湊的金鑰
        var apiKey = createApiKeyWithHash(rawKey, keyPrefix);
        when(apiKeyRepository.findByKeyPrefix(keyPrefix)).thenReturn(Optional.of(apiKey));

        // 執行
        var result = apiKeyService.validateKey(rawKey);

        // 驗證
        assertThat(result).isPresent();
        assertThat(result.get().getName()).isEqualTo("test-key");
    }

    @Test
    void shouldReturnEmptyForInvalidKey() {
        // 準備
        when(apiKeyRepository.findByKeyPrefix("dmcp_invalid")).thenReturn(Optional.empty());  // 12 characters

        // 執行
        var result = apiKeyService.validateKey("dmcp_invalid123456789012345678");

        // 驗證
        assertThat(result).isEmpty();
    }

    @Test
    void shouldReturnEmptyForNullKey() {
        var result = apiKeyService.validateKey(null);
        assertThat(result).isEmpty();
    }

    @Test
    void shouldReturnEmptyForKeyWithoutPrefix() {
        var result = apiKeyService.validateKey("invalid_key_without_prefix");
        assertThat(result).isEmpty();
    }

    @Test
    void shouldRevokeApiKey() {
        // 準備
        String keyId = randomId();
        var apiKey = createApiKey("test-key", "dmcp_test1234");
        apiKey = new ApiKey(keyId, apiKey.getName(), apiKey.getKeyHash(), apiKey.getKeyPrefix(),
                apiKey.getStatus(), apiKey.getRateLimit(), apiKey.getExpiresAt(), apiKey.getLastUsedAt(),
                apiKey.getCreatedBy(), 0L, apiKey.getCreatedAt(), apiKey.getUpdatedAt());

        when(apiKeyRepository.findById(keyId)).thenReturn(Optional.of(apiKey));
        when(apiKeyRepository.save(any(ApiKey.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // 執行
        apiKeyService.revokeKey(keyId);

        // 驗證
        ArgumentCaptor<ApiKey> captor = ArgumentCaptor.forClass(ApiKey.class);
        verify(apiKeyRepository).save(captor.capture());

        assertThat(captor.getValue().getStatus()).isEqualTo(ApiKeyStatus.REVOKED);
    }

    @Test
    void shouldThrowExceptionWhenRevokingNonexistentKey() {
        // 準備
        String keyId = randomId();
        when(apiKeyRepository.findById(keyId)).thenReturn(Optional.empty());

        // 執行 & 驗證
        assertThatThrownBy(() -> apiKeyService.revokeKey(keyId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining(keyId);
    }

    private ApiKey createApiKey(String name, String keyPrefix) {
        var now = OffsetDateTime.now();
        return new ApiKey(
                randomId(),
                name,
                "$2a$10$dummyhash",  // 假的雜湊
                keyPrefix,
                ApiKeyStatus.ACTIVE,
                1000,
                null,
                null,
                "system",
                0L,         // version（模擬從資料庫讀取）
                now,        // createdAt
                now         // updatedAt
        );
    }

    private ApiKey createApiKeyWithHash(String rawKey, String keyPrefix) {
        // 使用真正的 BCrypt 雜湊
        org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder encoder =
                new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder();
        String hash = encoder.encode(rawKey);
        var now = OffsetDateTime.now();

        return new ApiKey(
                randomId(),
                "test-key",
                hash,
                keyPrefix,
                ApiKeyStatus.ACTIVE,
                1000,
                null,
                null,
                "system",
                0L,         // version（模擬從資料庫讀取）
                now,        // createdAt
                now         // updatedAt
        );
    }
}
