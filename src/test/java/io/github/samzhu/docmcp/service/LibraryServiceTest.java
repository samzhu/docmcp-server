package io.github.samzhu.docmcp.service;

import io.github.samzhu.docmcp.config.KnownDocsPathsProperties;
import io.github.samzhu.docmcp.domain.enums.SourceType;
import io.github.samzhu.docmcp.domain.enums.VersionStatus;
import io.github.samzhu.docmcp.domain.exception.LibraryNotFoundException;
import io.github.samzhu.docmcp.domain.model.Library;
import io.github.samzhu.docmcp.domain.model.LibraryVersion;
import io.github.samzhu.docmcp.infrastructure.github.GitHubClient;
import io.github.samzhu.docmcp.repository.LibraryRepository;
import io.github.samzhu.docmcp.repository.LibraryVersionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

/**
 * LibraryService 單元測試
 */
@ExtendWith(MockitoExtension.class)
class LibraryServiceTest {

    @Mock
    private LibraryRepository libraryRepository;

    @Mock
    private LibraryVersionRepository libraryVersionRepository;

    @Mock
    private GitHubClient gitHubClient;

    @Mock
    private KnownDocsPathsProperties knownDocsPathsProperties;

    @Mock
    private SyncService syncService;

    @Mock
    private VersionService versionService;

    private LibraryService libraryService;

    @BeforeEach
    void setUp() {
        libraryService = new LibraryService(
                libraryRepository,
                libraryVersionRepository,
                gitHubClient,
                knownDocsPathsProperties,
                syncService,
                versionService
        );
    }

    @Test
    void shouldListAllLibraries() {
        // 測試列出所有函式庫
        var libraries = List.of(
                createLibrary("react", "React", "frontend"),
                createLibrary("spring-boot", "Spring Boot", "backend")
        );
        when(libraryRepository.findAll()).thenReturn(libraries);

        var result = libraryService.listLibraries(null);

        assertThat(result).hasSize(2);
    }

    @Test
    void shouldListLibrariesByCategory() {
        // 測試根據分類列出函式庫
        var libraries = List.of(
                createLibrary("react", "React", "frontend"),
                createLibrary("vue", "Vue.js", "frontend")
        );
        when(libraryRepository.findByCategory("frontend")).thenReturn(libraries);

        var result = libraryService.listLibraries("frontend");

        assertThat(result).hasSize(2);
        assertThat(result).allMatch(lib -> lib.category().equals("frontend"));
    }

    @Test
    void shouldResolveLibraryWithLatestVersion() {
        // 測試解析函式庫的最新版本
        // LibraryService 委派版本解析給 VersionService
        var library = createLibrary("spring-boot", "Spring Boot", "backend");
        var version = createVersion(library.id(), "3.2.0", true);
        when(libraryRepository.findByName("spring-boot")).thenReturn(Optional.of(library));
        when(versionService.resolveVersion(library.id(), null)).thenReturn(version);

        var result = libraryService.resolveLibrary("spring-boot", null);

        assertThat(result.library()).isEqualTo(library);
        assertThat(result.version()).isEqualTo(version);
        assertThat(result.resolvedVersion()).isEqualTo("3.2.0");
    }

    @Test
    void shouldResolveLibraryWithSpecificVersion() {
        // 測試解析函式庫的指定版本
        // LibraryService 委派版本解析給 VersionService
        var library = createLibrary("spring-boot", "Spring Boot", "backend");
        var version = createVersion(library.id(), "3.1.0", false);
        when(libraryRepository.findByName("spring-boot")).thenReturn(Optional.of(library));
        when(versionService.resolveVersion(library.id(), "3.1.0")).thenReturn(version);

        var result = libraryService.resolveLibrary("spring-boot", "3.1.0");

        assertThat(result.resolvedVersion()).isEqualTo("3.1.0");
    }

    @Test
    void shouldThrowExceptionWhenLibraryNotFound() {
        // 測試當函式庫不存在時拋出例外
        when(libraryRepository.findByName("nonexistent")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> libraryService.resolveLibrary("nonexistent", null))
                .isInstanceOf(LibraryNotFoundException.class)
                .hasMessageContaining("nonexistent");
    }

    @Test
    void shouldGetLibraryVersions() {
        // 測試取得函式庫的所有版本
        // LibraryService 委派版本查詢給 VersionService
        var library = createLibrary("spring-boot", "Spring Boot", "backend");
        var versions = List.of(
                createVersion(library.id(), "3.2.0", true),
                createVersion(library.id(), "3.1.0", false),
                createVersion(library.id(), "2.7.0", false)
        );
        when(versionService.getVersionsByLibraryName("spring-boot")).thenReturn(versions);

        var result = libraryService.getLibraryVersions("spring-boot");

        assertThat(result).hasSize(3);
    }

    /**
     * 建立測試用的函式庫
     */
    private Library createLibrary(String name, String displayName, String category) {
        return new Library(
                UUID.randomUUID(),
                name,
                displayName,
                null,
                SourceType.GITHUB,
                null,
                category,
                null,
                null,
                null
        );
    }

    /**
     * 建立測試用的版本
     */
    private LibraryVersion createVersion(UUID libraryId, String version, boolean isLatest) {
        return new LibraryVersion(
                UUID.randomUUID(),
                libraryId,
                version,
                isLatest,
                false,
                VersionStatus.ACTIVE,
                null,
                null,
                null,
                null
        );
    }
}
