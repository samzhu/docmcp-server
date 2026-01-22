package io.github.samzhu.docmcp.scheduler;

import com.github.f4b6a3.tsid.TsidCreator;
import io.github.samzhu.docmcp.config.FeatureFlags;
import io.github.samzhu.docmcp.domain.enums.SourceType;
import io.github.samzhu.docmcp.domain.enums.VersionStatus;
import io.github.samzhu.docmcp.domain.model.Library;
import io.github.samzhu.docmcp.domain.model.LibraryVersion;
import io.github.samzhu.docmcp.repository.LibraryRepository;
import io.github.samzhu.docmcp.repository.LibraryVersionRepository;
import io.github.samzhu.docmcp.service.SyncService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * SyncScheduler 同步排程器測試
 */
@ExtendWith(MockitoExtension.class)
class SyncSchedulerTest {

    @Mock
    private SyncService syncService;

    @Mock
    private LibraryRepository libraryRepository;

    @Mock
    private LibraryVersionRepository versionRepository;

    @Mock
    private FeatureFlags featureFlags;

    private SyncScheduler syncScheduler;

    /**
     * 產生隨機 ID
     */
    private String randomId() {
        return TsidCreator.getTsid().toString();
    }

    @BeforeEach
    void setUp() {
        syncScheduler = new SyncScheduler(syncService, libraryRepository, versionRepository, featureFlags);
    }

    @Test
    void shouldSkipSyncWhenFeatureDisabled() {
        // Arrange
        when(featureFlags.isSyncScheduling()).thenReturn(false);

        // Act
        syncScheduler.scheduledSync();

        // Assert
        verify(libraryRepository, never()).findAll();
        verify(syncService, never()).syncFromGitHub(any(), anyString(), anyString(), anyString(), anyString());
    }

    @Test
    void shouldSyncAllLibrariesWhenFeatureEnabled() {
        // Arrange
        when(featureFlags.isSyncScheduling()).thenReturn(true);

        String libraryId = randomId();
        Library library = createLibrary(libraryId, "spring-boot", "Spring Boot", SourceType.GITHUB,
                "https://github.com/spring-projects/spring-boot");

        String versionId = randomId();
        LibraryVersion version = createVersion(versionId, libraryId, "3.2.0", true);

        when(libraryRepository.findAll()).thenReturn(List.of(library));
        when(versionRepository.findByLibraryId(libraryId)).thenReturn(List.of(version));

        // Act
        syncScheduler.scheduledSync();

        // Assert
        verify(libraryRepository).findAll();
        verify(versionRepository).findByLibraryId(libraryId);
    }

    @Test
    void shouldSkipNonGitHubLibraries() {
        // Arrange
        when(featureFlags.isSyncScheduling()).thenReturn(true);

        String libraryId = randomId();
        Library library = createLibrary(libraryId, "local-docs", "Local Docs", SourceType.LOCAL, null);

        when(libraryRepository.findAll()).thenReturn(List.of(library));

        // Act
        syncScheduler.scheduledSync();

        // Assert
        verify(libraryRepository).findAll();
        verify(versionRepository, never()).findByLibraryId(any());
    }

    private Library createLibrary(String id, String name, String displayName, SourceType sourceType, String sourceUrl) {
        return new Library(id, name, displayName, null, sourceType, sourceUrl, null, null, 0L, null, null);
    }

    private LibraryVersion createVersion(String id, String libraryId, String version, boolean isLatest) {
        return new LibraryVersion(id, libraryId, version, isLatest, false, VersionStatus.ACTIVE, "docs", null, 0L, null, null);
    }
}
