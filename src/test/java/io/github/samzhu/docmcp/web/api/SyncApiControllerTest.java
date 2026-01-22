package io.github.samzhu.docmcp.web.api;

import com.github.f4b6a3.tsid.TsidCreator;
import io.github.samzhu.docmcp.TestConfig;
import io.github.samzhu.docmcp.TestcontainersConfiguration;
import io.github.samzhu.docmcp.domain.enums.SyncStatus;
import io.github.samzhu.docmcp.domain.model.SyncHistory;
import io.github.samzhu.docmcp.service.SyncService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * SyncApiController 測試
 */
@SpringBootTest
@Import({TestcontainersConfiguration.class, TestConfig.class})
@ActiveProfiles("test")
class SyncApiControllerTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    private MockMvc mockMvc;

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
    private SyncService syncService;

    @Test
    @WithMockUser
    void shouldGetSyncHistory() throws Exception {
        var histories = List.of(
                createSyncHistory(SyncStatus.SUCCESS, 10, 50),
                createSyncHistory(SyncStatus.RUNNING, 5, 25)
        );
        when(syncService.getSyncHistory(any(), eq(10))).thenReturn(histories);

        mockMvc.perform(get("/api/sync/history"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].status").value("SUCCESS"))
                .andExpect(jsonPath("$[0].documentsProcessed").value(10));
    }

    @Test
    @WithMockUser
    void shouldGetSyncHistoryByVersionId() throws Exception {
        var versionId = randomId();
        var histories = List.of(
                createSyncHistoryWithVersionId(versionId, SyncStatus.SUCCESS, 10, 50)
        );
        when(syncService.getSyncHistory(eq(versionId), eq(10))).thenReturn(histories);

        mockMvc.perform(get("/api/sync/history")
                        .param("versionId", versionId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    @WithMockUser
    void shouldGetSyncHistoryWithCustomLimit() throws Exception {
        var histories = List.of(
                createSyncHistory(SyncStatus.SUCCESS, 10, 50)
        );
        when(syncService.getSyncHistory(any(), eq(5))).thenReturn(histories);

        mockMvc.perform(get("/api/sync/history")
                        .param("limit", "5"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser
    void shouldGetSyncStatusById() throws Exception {
        var syncId = randomId();
        var history = createSyncHistory(SyncStatus.SUCCESS, 15, 75);
        when(syncService.getSyncStatus(syncId)).thenReturn(Optional.of(history));

        mockMvc.perform(get("/api/sync/{id}", syncId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.documentsProcessed").value(15))
                .andExpect(jsonPath("$.chunksCreated").value(75));
    }

    @Test
    @WithMockUser
    void shouldReturn404WhenSyncStatusNotFound() throws Exception {
        var syncId = randomId();
        when(syncService.getSyncStatus(syncId)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/sync/{id}", syncId))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser
    void shouldShowErrorMessageForFailedSync() throws Exception {
        var syncId = randomId();
        var now = OffsetDateTime.now();
        var history = new SyncHistory(
                syncId,
                randomId(),
                SyncStatus.FAILED,
                now.minusMinutes(5),
                now,
                5,
                20,
                "Connection timeout",
                Map.of(),
                0L,                    // version（模擬從資料庫讀取）
                now.minusMinutes(5),   // createdAt
                now                    // updatedAt
        );
        when(syncService.getSyncStatus(syncId)).thenReturn(Optional.of(history));

        mockMvc.perform(get("/api/sync/{id}", syncId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("FAILED"))
                .andExpect(jsonPath("$.errorMessage").value("Connection timeout"));
    }

    private SyncHistory createSyncHistory(SyncStatus status, int docsProcessed, int chunksCreated) {
        var now = OffsetDateTime.now();
        return new SyncHistory(
                randomId(),
                randomId(),
                status,
                now.minusMinutes(10),
                status == SyncStatus.RUNNING ? null : now,
                docsProcessed,
                chunksCreated,
                null,
                Map.of(),
                0L,                     // version（模擬從資料庫讀取）
                now.minusMinutes(10),   // createdAt
                now                     // updatedAt
        );
    }

    private SyncHistory createSyncHistoryWithVersionId(String versionId, SyncStatus status,
                                                        int docsProcessed, int chunksCreated) {
        var now = OffsetDateTime.now();
        return new SyncHistory(
                randomId(),
                versionId,
                status,
                now.minusMinutes(10),
                now,
                docsProcessed,
                chunksCreated,
                null,
                Map.of(),
                0L,                     // version（模擬從資料庫讀取）
                now.minusMinutes(10),   // createdAt
                now                     // updatedAt
        );
    }
}
