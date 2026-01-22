package io.github.samzhu.docmcp.web.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.f4b6a3.tsid.TsidCreator;
import io.github.samzhu.docmcp.TestConfig;
import io.github.samzhu.docmcp.TestcontainersConfiguration;
import io.github.samzhu.docmcp.domain.enums.ApiKeyStatus;
import io.github.samzhu.docmcp.domain.model.ApiKey;
import io.github.samzhu.docmcp.security.ApiKeyService;
import io.github.samzhu.docmcp.web.dto.CreateApiKeyRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * ApiKeyController 測試
 */
@SpringBootTest
@Import({TestcontainersConfiguration.class, TestConfig.class})
@ActiveProfiles("test")
class ApiKeyControllerTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    /**
     * 產生隨機 ID
     */
    private String randomId() {
        return TsidCreator.getTsid().toString();
    }

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(webApplicationContext)
                .apply(springSecurity())
                .build();
    }

    @MockitoBean
    private ApiKeyService apiKeyService;

    @Test
    @WithMockUser(roles = "API_KEY")
    void shouldCreateApiKey() throws Exception {
        var request = new CreateApiKeyRequest("test-key", null, 1000);
        var generated = new ApiKeyService.GeneratedApiKey(
                randomId(),
                "test-key",
                "dmcp_abcdefgh12345678901234567890",
                "dmcp_abcdefg"
        );

        when(apiKeyService.generateKey(eq("test-key"), any(), any(), eq(1000)))
                .thenReturn(generated);

        mockMvc.perform(post("/api/keys")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("test-key"))
                .andExpect(jsonPath("$.rawKey").value("dmcp_abcdefgh12345678901234567890"))
                .andExpect(jsonPath("$.keyPrefix").value("dmcp_abcdefg"));
    }

    @Test
    @WithMockUser(roles = "API_KEY")
    void shouldListApiKeys() throws Exception {
        var keys = List.of(
                createApiKey(randomId(), "key-1", "dmcp_key11234"),
                createApiKey(randomId(), "key-2", "dmcp_key22345")
        );

        when(apiKeyService.listKeys()).thenReturn(keys);

        mockMvc.perform(get("/api/keys"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].name").value("key-1"))
                .andExpect(jsonPath("$[1].name").value("key-2"));
    }

    @Test
    @WithMockUser(roles = "API_KEY")
    void shouldGetApiKeyById() throws Exception {
        var id = randomId();
        var apiKey = createApiKey(id, "test-key", "dmcp_test1234");

        when(apiKeyService.getKeyById(id)).thenReturn(Optional.of(apiKey));

        mockMvc.perform(get("/api/keys/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("test-key"))
                .andExpect(jsonPath("$.keyPrefix").value("dmcp_test1234"));
    }

    @Test
    @WithMockUser(roles = "API_KEY")
    void shouldReturn404WhenApiKeyNotFound() throws Exception {
        var id = randomId();

        when(apiKeyService.getKeyById(id)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/keys/{id}", id))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = "API_KEY")
    void shouldRevokeApiKey() throws Exception {
        var id = randomId();

        doNothing().when(apiKeyService).revokeKey(id);

        mockMvc.perform(delete("/api/keys/{id}", id)
                        .with(csrf()))
                .andExpect(status().isNoContent());

        verify(apiKeyService).revokeKey(id);
    }

    @Test
    @WithMockUser(roles = "API_KEY")
    void shouldReturn400WhenCreatingKeyWithBlankName() throws Exception {
        var request = new CreateApiKeyRequest("", null, null);

        mockMvc.perform(post("/api/keys")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    private ApiKey createApiKey(String id, String name, String keyPrefix) {
        var now = OffsetDateTime.now();
        return new ApiKey(
                id,
                name,
                "$2a$10$dummyhash",
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
