package io.github.samzhu.docmcp.security;

import io.github.samzhu.docmcp.domain.enums.ApiKeyStatus;
import io.github.samzhu.docmcp.domain.model.ApiKey;
import io.github.samzhu.docmcp.repository.ApiKeyRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

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
    private ApiKeyRepository apiKeyRepository;

    private ApiKeyService apiKeyService;

    @BeforeEach
    void setUp() {
        apiKeyService = new ApiKeyService(apiKeyRepository);
    }

    @Test
    void shouldGenerateNewApiKey() {
        // 準備
        when(apiKeyRepository.findByName("test-key")).thenReturn(Optional.empty());
        when(apiKeyRepository.save(any(ApiKey.class))).thenAnswer(invocation -> {
            ApiKey apiKey = invocation.getArgument(0);
            return new ApiKey(
                    UUID.randomUUID(),
                    apiKey.name(),
                    apiKey.keyHash(),
                    apiKey.keyPrefix(),
                    apiKey.status(),
                    apiKey.rateLimit(),
                    apiKey.expiresAt(),
                    apiKey.lastUsedAt(),
                    OffsetDateTime.now(),
                    apiKey.createdBy()
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
        assertThat(savedKey.name()).isEqualTo("test-key");
        assertThat(savedKey.status()).isEqualTo(ApiKeyStatus.ACTIVE);
        assertThat(savedKey.createdBy()).isEqualTo("admin");
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
        assertThat(result.get().name()).isEqualTo("test-key");
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
        UUID keyId = UUID.randomUUID();
        var apiKey = createApiKey("test-key", "dmcp_test1234");
        apiKey = new ApiKey(keyId, apiKey.name(), apiKey.keyHash(), apiKey.keyPrefix(),
                apiKey.status(), apiKey.rateLimit(), apiKey.expiresAt(), apiKey.lastUsedAt(),
                apiKey.createdAt(), apiKey.createdBy());

        when(apiKeyRepository.findById(keyId)).thenReturn(Optional.of(apiKey));
        when(apiKeyRepository.save(any(ApiKey.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // 執行
        apiKeyService.revokeKey(keyId);

        // 驗證
        ArgumentCaptor<ApiKey> captor = ArgumentCaptor.forClass(ApiKey.class);
        verify(apiKeyRepository).save(captor.capture());

        assertThat(captor.getValue().status()).isEqualTo(ApiKeyStatus.REVOKED);
    }

    @Test
    void shouldThrowExceptionWhenRevokingNonexistentKey() {
        // 準備
        UUID keyId = UUID.randomUUID();
        when(apiKeyRepository.findById(keyId)).thenReturn(Optional.empty());

        // 執行 & 驗證
        assertThatThrownBy(() -> apiKeyService.revokeKey(keyId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining(keyId.toString());
    }

    private ApiKey createApiKey(String name, String keyPrefix) {
        return new ApiKey(
                UUID.randomUUID(),
                name,
                "$2a$10$dummyhash",  // 假的雜湊
                keyPrefix,
                ApiKeyStatus.ACTIVE,
                1000,
                null,
                null,
                OffsetDateTime.now(),
                "system"
        );
    }

    private ApiKey createApiKeyWithHash(String rawKey, String keyPrefix) {
        // 使用真正的 BCrypt 雜湊
        org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder encoder =
                new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder();
        String hash = encoder.encode(rawKey);

        return new ApiKey(
                UUID.randomUUID(),
                "test-key",
                hash,
                keyPrefix,
                ApiKeyStatus.ACTIVE,
                1000,
                null,
                null,
                OffsetDateTime.now(),
                "system"
        );
    }
}
