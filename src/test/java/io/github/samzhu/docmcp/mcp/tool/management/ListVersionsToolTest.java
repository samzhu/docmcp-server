package io.github.samzhu.docmcp.mcp.tool.management;

import io.github.samzhu.docmcp.domain.enums.VersionStatus;
import io.github.samzhu.docmcp.domain.exception.LibraryNotFoundException;
import io.github.samzhu.docmcp.domain.model.Library;
import io.github.samzhu.docmcp.domain.model.LibraryVersion;
import io.github.samzhu.docmcp.service.LibraryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;

import com.github.f4b6a3.tsid.TsidCreator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

/**
 * ListVersionsTool 單元測試
 */
@ExtendWith(MockitoExtension.class)
class ListVersionsToolTest {

    @Mock
    private LibraryService libraryService;

    private ListVersionsTool listVersionsTool;

    @BeforeEach
    void setUp() {
        listVersionsTool = new ListVersionsTool(libraryService);
    }

    @Test
    @DisplayName("應回傳指定函式庫的所有版本（包含棄用）")
    void shouldReturnAllVersionsForLibrary() {
        // Arrange
        var libraryId = randomId();
        var library = createLibrary(libraryId, "spring-boot", "Spring Boot");
        var versions = List.of(
                createVersion(libraryId, "3.2.0", true, VersionStatus.ACTIVE),
                createVersion(libraryId, "3.1.0", false, VersionStatus.ACTIVE),
                createVersion(libraryId, "2.7.0", false, VersionStatus.DEPRECATED)
        );

        when(libraryService.getLibraryById(libraryId)).thenReturn(library);
        when(libraryService.getLibraryVersionsById(libraryId)).thenReturn(versions);

        // Act - 使用 includeDeprecated=true 來取得所有版本
        var result = listVersionsTool.listVersions(libraryId, true);

        // Assert
        assertThat(result.libraryId()).isEqualTo(libraryId);
        assertThat(result.libraryName()).isEqualTo("spring-boot");
        assertThat(result.versions()).hasSize(3);
        assertThat(result.total()).isEqualTo(3);
    }

    @Test
    @DisplayName("應只回傳非棄用版本（當 includeDeprecated 為 false）")
    void shouldExcludeDeprecatedVersionsWhenFlagIsFalse() {
        // Arrange
        var libraryId = randomId();
        var library = createLibrary(libraryId, "spring-boot", "Spring Boot");
        var versions = List.of(
                createVersion(libraryId, "3.2.0", true, VersionStatus.ACTIVE),
                createVersion(libraryId, "3.1.0", false, VersionStatus.ACTIVE),
                createVersion(libraryId, "2.7.0", false, VersionStatus.DEPRECATED),
                createVersion(libraryId, "2.5.0", false, VersionStatus.EOL)
        );

        when(libraryService.getLibraryById(libraryId)).thenReturn(library);
        when(libraryService.getLibraryVersionsById(libraryId)).thenReturn(versions);

        // Act
        var result = listVersionsTool.listVersions(libraryId, false);

        // Assert
        assertThat(result.versions()).hasSize(2);
        assertThat(result.versions())
                .allMatch(v -> "ACTIVE".equals(v.status()));
    }

    @Test
    @DisplayName("應回傳包含棄用版本（當 includeDeprecated 為 true）")
    void shouldIncludeDeprecatedVersionsWhenFlagIsTrue() {
        // Arrange
        var libraryId = randomId();
        var library = createLibrary(libraryId, "spring-boot", "Spring Boot");
        var versions = List.of(
                createVersion(libraryId, "3.2.0", true, VersionStatus.ACTIVE),
                createVersion(libraryId, "2.7.0", false, VersionStatus.DEPRECATED),
                createVersion(libraryId, "2.5.0", false, VersionStatus.EOL)
        );

        when(libraryService.getLibraryById(libraryId)).thenReturn(library);
        when(libraryService.getLibraryVersionsById(libraryId)).thenReturn(versions);

        // Act
        var result = listVersionsTool.listVersions(libraryId, true);

        // Assert
        assertThat(result.versions()).hasSize(3);
        assertThat(result.total()).isEqualTo(3);
    }

    @Test
    @DisplayName("當 includeDeprecated 為 null 時應使用預設值 false")
    void shouldUseDefaultValueWhenIncludeDeprecatedIsNull() {
        // Arrange
        var libraryId = randomId();
        var library = createLibrary(libraryId, "react", "React");
        var versions = List.of(
                createVersion(libraryId, "18.2.0", true, VersionStatus.ACTIVE),
                createVersion(libraryId, "17.0.0", false, VersionStatus.DEPRECATED)
        );

        when(libraryService.getLibraryById(libraryId)).thenReturn(library);
        when(libraryService.getLibraryVersionsById(libraryId)).thenReturn(versions);

        // Act
        var result = listVersionsTool.listVersions(libraryId, null);

        // Assert
        assertThat(result.versions()).hasSize(1);
        assertThat(result.versions().get(0).version()).isEqualTo("18.2.0");
    }

    @Test
    @DisplayName("當函式庫不存在時應拋出例外")
    void shouldThrowExceptionWhenLibraryNotFound() {
        // Arrange
        var libraryId = randomId();
        when(libraryService.getLibraryById(libraryId))
                .thenThrow(LibraryNotFoundException.byId(libraryId));

        // Act & Assert
        assertThatThrownBy(() -> listVersionsTool.listVersions(libraryId, false))
                .isInstanceOf(LibraryNotFoundException.class);
    }

    @Test
    @DisplayName("當函式庫沒有版本時應回傳空列表")
    void shouldReturnEmptyListWhenNoVersions() {
        // Arrange
        var libraryId = randomId();
        var library = createLibrary(libraryId, "new-lib", "New Library");

        when(libraryService.getLibraryById(libraryId)).thenReturn(library);
        when(libraryService.getLibraryVersionsById(libraryId)).thenReturn(List.of());

        // Act
        var result = listVersionsTool.listVersions(libraryId, false);

        // Assert
        assertThat(result.versions()).isEmpty();
        assertThat(result.total()).isEqualTo(0);
    }

    // 產生隨機 ID 的輔助方法
    private String randomId() {
        return TsidCreator.getTsid().toString();
    }

    private Library createLibrary(String id, String name, String displayName) {
        return new Library(id, name, displayName, null, null, null, null, null, null, null, null);
    }

    private LibraryVersion createVersion(String libraryId, String version, boolean isLatest, VersionStatus status) {
        return new LibraryVersion(
                randomId(),
                libraryId,
                version,
                isLatest,
                false,
                status,
                null,
                LocalDate.now(),
                null,  // entityVersion
                null,
                null
        );
    }
}
