package io.github.samzhu.docmcp.mcp.tool.discovery;

import com.github.f4b6a3.tsid.TsidCreator;
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

    /**
     * 產生隨機 ID
     */
    private String randomId() {
        return TsidCreator.getTsid().toString();
    }

    @BeforeEach
    void setUp() {
        resolveLibraryTool = new ResolveLibraryTool(libraryService);
    }

    @Test
    void shouldResolveLibraryWithLatestVersion() {
        // 測試解析函式庫的最新版本
        var library = createLibrary("spring-boot", "Spring Boot");
        var version = createVersion(library.getId(), "3.2.0", true);
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
        var version = createVersion(library.getId(), "3.1.0", false);
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
                randomId(),
                name,
                displayName,
                null,
                SourceType.GITHUB,
                null,
                null,
                null,
                0L,     // version（模擬從資料庫讀取）
                null,
                null
        );
    }

    /**
     * 建立測試用的版本
     */
    private LibraryVersion createVersion(String libraryId, String version, boolean isLatest) {
        return new LibraryVersion(
                randomId(),
                libraryId,
                version,
                isLatest,
                false,
                VersionStatus.ACTIVE,
                null,
                null,
                0L,     // entityVersion（模擬從資料庫讀取）
                null,
                null
        );
    }
}
