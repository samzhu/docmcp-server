package io.github.samzhu.docmcp.mcp.tool.management;

import io.github.samzhu.docmcp.domain.enums.SyncStatus;
import io.github.samzhu.docmcp.domain.enums.VersionStatus;
import io.github.samzhu.docmcp.domain.exception.LibraryNotFoundException;
import io.github.samzhu.docmcp.domain.model.Library;
import io.github.samzhu.docmcp.domain.model.LibraryVersion;
import io.github.samzhu.docmcp.domain.model.SyncHistory;
import io.github.samzhu.docmcp.repository.SyncHistoryRepository;
import io.github.samzhu.docmcp.service.LibraryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

/**
 * GetSyncStatusTool 單元測試
 */
@ExtendWith(MockitoExtension.class)
class GetSyncStatusToolTest {

    @Mock
    private LibraryService libraryService;

    @Mock
    private SyncHistoryRepository syncHistoryRepository;

    private GetSyncStatusTool getSyncStatusTool;

    private static final int DEFAULT_LIMIT = 5;

    @BeforeEach
    void setUp() {
        getSyncStatusTool = new GetSyncStatusTool(libraryService, syncHistoryRepository);
    }

    @Test
    @DisplayName("應回傳函式庫的同步狀態與歷史")
    void shouldReturnSyncStatusAndHistory() {
        // Arrange
        var libraryId = UUID.randomUUID();
        var versionId = UUID.randomUUID();
        var library = createLibrary(libraryId, "spring-boot", "Spring Boot");
        var version = createVersion(versionId, libraryId, "3.2.0", true);
        var resolvedLibrary = new LibraryService.ResolvedLibrary(library, version, "3.2.0");

        var latestSync = createSyncHistory(versionId, SyncStatus.SUCCESS, 10, 50);
        var syncHistory = List.of(
                latestSync,
                createSyncHistory(versionId, SyncStatus.SUCCESS, 8, 40),
                createSyncHistory(versionId, SyncStatus.FAILED, 0, 0)
        );

        when(libraryService.resolveLibrary("spring-boot", null)).thenReturn(resolvedLibrary);
        when(syncHistoryRepository.hasRunningSyncTask(versionId)).thenReturn(false);
        when(syncHistoryRepository.findLatestByVersionId(versionId)).thenReturn(Optional.of(latestSync));
        when(syncHistoryRepository.findByVersionIdOrderByStartedAtDescLimit(versionId, DEFAULT_LIMIT))
                .thenReturn(syncHistory);

        // Act
        var result = getSyncStatusTool.getSyncStatus("spring-boot", null);

        // Assert
        assertThat(result.libraryId()).isEqualTo(libraryId.toString());
        assertThat(result.libraryName()).isEqualTo("spring-boot");
        assertThat(result.versionId()).isEqualTo(versionId.toString());
        assertThat(result.version()).isEqualTo("3.2.0");
        assertThat(result.isRunning()).isFalse();
        assertThat(result.latestSync()).isNotNull();
        assertThat(result.latestSync().status()).isEqualTo("SUCCESS");
        assertThat(result.recentSyncs()).hasSize(3);
    }

    @Test
    @DisplayName("當有正在執行的同步任務時應標記 isRunning 為 true")
    void shouldIndicateRunningTaskWhenSyncIsInProgress() {
        // Arrange
        var libraryId = UUID.randomUUID();
        var versionId = UUID.randomUUID();
        var library = createLibrary(libraryId, "react", "React");
        var version = createVersion(versionId, libraryId, "18.2.0", true);
        var resolvedLibrary = new LibraryService.ResolvedLibrary(library, version, "18.2.0");

        var runningSync = createSyncHistory(versionId, SyncStatus.RUNNING, 5, 20);

        when(libraryService.resolveLibrary("react", null)).thenReturn(resolvedLibrary);
        when(syncHistoryRepository.hasRunningSyncTask(versionId)).thenReturn(true);
        when(syncHistoryRepository.findLatestByVersionId(versionId)).thenReturn(Optional.of(runningSync));
        when(syncHistoryRepository.findByVersionIdOrderByStartedAtDescLimit(versionId, DEFAULT_LIMIT))
                .thenReturn(List.of(runningSync));

        // Act
        var result = getSyncStatusTool.getSyncStatus("react", null);

        // Assert
        assertThat(result.isRunning()).isTrue();
        assertThat(result.latestSync().status()).isEqualTo("RUNNING");
    }

    @Test
    @DisplayName("應支援指定版本查詢同步狀態")
    void shouldSupportVersionSpecificQuery() {
        // Arrange
        var libraryId = UUID.randomUUID();
        var versionId = UUID.randomUUID();
        var library = createLibrary(libraryId, "spring-boot", "Spring Boot");
        var version = createVersion(versionId, libraryId, "3.1.0", false);
        var resolvedLibrary = new LibraryService.ResolvedLibrary(library, version, "3.1.0");

        var latestSync = createSyncHistory(versionId, SyncStatus.SUCCESS, 12, 60);

        when(libraryService.resolveLibrary("spring-boot", "3.1.0")).thenReturn(resolvedLibrary);
        when(syncHistoryRepository.hasRunningSyncTask(versionId)).thenReturn(false);
        when(syncHistoryRepository.findLatestByVersionId(versionId)).thenReturn(Optional.of(latestSync));
        when(syncHistoryRepository.findByVersionIdOrderByStartedAtDescLimit(versionId, DEFAULT_LIMIT))
                .thenReturn(List.of(latestSync));

        // Act
        var result = getSyncStatusTool.getSyncStatus("spring-boot", "3.1.0");

        // Assert
        assertThat(result.version()).isEqualTo("3.1.0");
        assertThat(result.versionId()).isEqualTo(versionId.toString());
    }

    @Test
    @DisplayName("當沒有同步歷史時應回傳空資訊")
    void shouldReturnEmptyWhenNoSyncHistory() {
        // Arrange
        var libraryId = UUID.randomUUID();
        var versionId = UUID.randomUUID();
        var library = createLibrary(libraryId, "new-lib", "New Library");
        var version = createVersion(versionId, libraryId, "1.0.0", true);
        var resolvedLibrary = new LibraryService.ResolvedLibrary(library, version, "1.0.0");

        when(libraryService.resolveLibrary("new-lib", null)).thenReturn(resolvedLibrary);
        when(syncHistoryRepository.hasRunningSyncTask(versionId)).thenReturn(false);
        when(syncHistoryRepository.findLatestByVersionId(versionId)).thenReturn(Optional.empty());
        when(syncHistoryRepository.findByVersionIdOrderByStartedAtDescLimit(versionId, DEFAULT_LIMIT))
                .thenReturn(List.of());

        // Act
        var result = getSyncStatusTool.getSyncStatus("new-lib", null);

        // Assert
        assertThat(result.latestSync()).isNull();
        assertThat(result.recentSyncs()).isEmpty();
        assertThat(result.isRunning()).isFalse();
    }

    @Test
    @DisplayName("當函式庫不存在時應拋出例外")
    void shouldThrowExceptionWhenLibraryNotFound() {
        // Arrange
        when(libraryService.resolveLibrary("non-existent", null))
                .thenThrow(LibraryNotFoundException.byName("non-existent"));

        // Act & Assert
        assertThatThrownBy(() -> getSyncStatusTool.getSyncStatus("non-existent", null))
                .isInstanceOf(LibraryNotFoundException.class);
    }

    private Library createLibrary(UUID id, String name, String displayName) {
        return new Library(id, name, displayName, null, null, null, null, null, null, null);
    }

    private LibraryVersion createVersion(UUID id, UUID libraryId, String version, boolean isLatest) {
        return new LibraryVersion(id, libraryId, version, isLatest, false, VersionStatus.ACTIVE, null, null, null, null);
    }

    private SyncHistory createSyncHistory(UUID versionId, SyncStatus status, int docs, int chunks) {
        return new SyncHistory(
                UUID.randomUUID(),
                versionId,
                status,
                OffsetDateTime.now().minusHours(1),
                status == SyncStatus.RUNNING ? null : OffsetDateTime.now(),
                docs,
                chunks,
                status == SyncStatus.FAILED ? "Sync failed due to network error" : null,
                Map.of()
        );
    }
}
