package io.github.samzhu.docmcp.web.api;

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
import java.util.UUID;

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
        var versionId = UUID.randomUUID();
        var histories = List.of(
                createSyncHistoryWithVersionId(versionId, SyncStatus.SUCCESS, 10, 50)
        );
        when(syncService.getSyncHistory(eq(versionId), eq(10))).thenReturn(histories);

        mockMvc.perform(get("/api/sync/history")
                        .param("versionId", versionId.toString()))
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
        var syncId = UUID.randomUUID();
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
        var syncId = UUID.randomUUID();
        when(syncService.getSyncStatus(syncId)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/sync/{id}", syncId))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser
    void shouldShowErrorMessageForFailedSync() throws Exception {
        var syncId = UUID.randomUUID();
        var history = new SyncHistory(
                syncId,
                UUID.randomUUID(),
                SyncStatus.FAILED,
                OffsetDateTime.now().minusMinutes(5),
                OffsetDateTime.now(),
                5,
                20,
                "Connection timeout",
                Map.of()
        );
        when(syncService.getSyncStatus(syncId)).thenReturn(Optional.of(history));

        mockMvc.perform(get("/api/sync/{id}", syncId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("FAILED"))
                .andExpect(jsonPath("$.errorMessage").value("Connection timeout"));
    }

    private SyncHistory createSyncHistory(SyncStatus status, int docsProcessed, int chunksCreated) {
        return new SyncHistory(
                UUID.randomUUID(),
                UUID.randomUUID(),
                status,
                OffsetDateTime.now().minusMinutes(10),
                status == SyncStatus.RUNNING ? null : OffsetDateTime.now(),
                docsProcessed,
                chunksCreated,
                null,
                Map.of()
        );
    }

    private SyncHistory createSyncHistoryWithVersionId(UUID versionId, SyncStatus status,
                                                        int docsProcessed, int chunksCreated) {
        return new SyncHistory(
                UUID.randomUUID(),
                versionId,
                status,
                OffsetDateTime.now().minusMinutes(10),
                OffsetDateTime.now(),
                docsProcessed,
                chunksCreated,
                null,
                Map.of()
        );
    }
}
