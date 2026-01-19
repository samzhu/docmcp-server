package io.github.samzhu.docmcp.mcp.tool.discovery;

import io.github.samzhu.docmcp.domain.enums.SourceType;
import io.github.samzhu.docmcp.domain.enums.VersionStatus;
import io.github.samzhu.docmcp.domain.exception.LibraryNotFoundException;
import io.github.samzhu.docmcp.domain.model.Library;
import io.github.samzhu.docmcp.domain.model.LibraryVersion;
import io.github.samzhu.docmcp.service.LibraryService;
import io.github.samzhu.docmcp.service.LibraryService.ResolvedLibrary;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

/**
 * ResolveLibraryTool 單元測試
 */
@ExtendWith(MockitoExtension.class)
class ResolveLibraryToolTest {

    @Mock
    private LibraryService libraryService;

    private ResolveLibraryTool resolveLibraryTool;

    @BeforeEach
    void setUp() {
        resolveLibraryTool = new ResolveLibraryTool(libraryService);
    }

    @Test
    void shouldResolveLibraryWithLatestVersion() {
        // 測試解析函式庫的最新版本
        var library = createLibrary("spring-boot", "Spring Boot");
        var version = createVersion(library.id(), "3.2.0", true);
        var resolved = new ResolvedLibrary(library, version, "3.2.0");
        when(libraryService.resolveLibrary("spring-boot", null)).thenReturn(resolved);

        var result = resolveLibraryTool.resolveLibrary("spring-boot", null);

        assertThat(result.name()).isEqualTo("spring-boot");
        assertThat(result.resolvedVersion()).isEqualTo("3.2.0");
        assertThat(result.status()).isEqualTo("ACTIVE");
    }

    @Test
    void shouldResolveLibraryWithSpecificVersion() {
        // 測試解析函式庫的指定版本
        var library = createLibrary("spring-boot", "Spring Boot");
        var version = createVersion(library.id(), "3.1.0", false);
        var resolved = new ResolvedLibrary(library, version, "3.1.0");
        when(libraryService.resolveLibrary("spring-boot", "3.1.0")).thenReturn(resolved);

        var result = resolveLibraryTool.resolveLibrary("spring-boot", "3.1.0");

        assertThat(result.resolvedVersion()).isEqualTo("3.1.0");
    }

    @Test
    void shouldThrowExceptionWhenLibraryNotFound() {
        // 測試當函式庫不存在時拋出例外
        when(libraryService.resolveLibrary("nonexistent", null))
                .thenThrow(LibraryNotFoundException.byName("nonexistent"));

        assertThatThrownBy(() -> resolveLibraryTool.resolveLibrary("nonexistent", null))
                .isInstanceOf(LibraryNotFoundException.class);
    }

    /**
     * 建立測試用的函式庫
     */
    private Library createLibrary(String name, String displayName) {
        return new Library(
                UUID.randomUUID(),
                name,
                displayName,
                null,
                SourceType.GITHUB,
                null,
                null,
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
                VersionStatus.ACTIVE,
                null,
                null,
                null,
                null
        );
    }
}
