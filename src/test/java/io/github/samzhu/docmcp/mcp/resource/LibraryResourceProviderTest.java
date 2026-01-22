package io.github.samzhu.docmcp.mcp.resource;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.samzhu.docmcp.domain.enums.SourceType;
import io.github.samzhu.docmcp.domain.enums.VersionStatus;
import io.github.samzhu.docmcp.domain.exception.LibraryNotFoundException;
import io.github.samzhu.docmcp.domain.model.Library;
import io.github.samzhu.docmcp.domain.model.LibraryVersion;
import io.github.samzhu.docmcp.service.LibraryService;
import io.modelcontextprotocol.spec.McpSchema.TextResourceContents;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import com.github.f4b6a3.tsid.TsidCreator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

/**
 * LibraryResourceProvider 單元測試
 */
@ExtendWith(MockitoExtension.class)
class LibraryResourceProviderTest {

    @Mock
    private LibraryService libraryService;

    private LibraryResourceProvider libraryResourceProvider;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        libraryResourceProvider = new LibraryResourceProvider(libraryService, objectMapper);
    }

    @Test
    @DisplayName("應回傳函式庫元資料")
    void shouldReturnLibraryMetadata() {
        // Arrange
        var libraryId = randomId();
        var versionId = randomId();
        var library = createLibrary(libraryId, "spring-boot", "Spring Boot",
                "Spring Boot Framework", SourceType.GITHUB, "https://github.com/spring-projects/spring-boot");
        var versions = List.of(
                createVersion(versionId, libraryId, "3.2.0", true, VersionStatus.ACTIVE)
        );

        when(libraryService.getLibraryById(libraryId)).thenReturn(library);
        when(libraryService.getLibraryVersionsById(libraryId)).thenReturn(versions);

        // Act
        var result = libraryResourceProvider.getLibraryResource(libraryId);

        // Assert
        assertThat(result.contents()).hasSize(1);
        var content = (TextResourceContents) result.contents().get(0);
        assertThat(content.uri()).isEqualTo("library://" + libraryId);
        assertThat(content.mimeType()).isEqualTo("application/json");
        assertThat(content.text()).contains("spring-boot");
        assertThat(content.text()).contains("Spring Boot");
        assertThat(content.text()).contains("3.2.0");
    }

    @Test
    @DisplayName("應透過名稱回傳函式庫元資料")
    void shouldReturnLibraryMetadataByName() {
        // Arrange
        var libraryId = randomId();
        var versionId = randomId();
        var library = createLibrary(libraryId, "react", "React",
                "React Framework", SourceType.GITHUB, "https://github.com/facebook/react");
        var version = createVersion(versionId, libraryId, "18.2.0", true, VersionStatus.ACTIVE);
        var resolvedLibrary = new LibraryService.ResolvedLibrary(library, version, "18.2.0");
        var versions = List.of(version);

        when(libraryService.resolveLibrary("react", null)).thenReturn(resolvedLibrary);
        when(libraryService.getLibraryById(libraryId)).thenReturn(library);
        when(libraryService.getLibraryVersionsById(libraryId)).thenReturn(versions);

        // Act
        var result = libraryResourceProvider.getLibraryByNameResource("react");

        // Assert
        assertThat(result.contents()).hasSize(1);
        var content = (TextResourceContents) result.contents().get(0);
        assertThat(content.text()).contains("react");
    }

    @Test
    @DisplayName("應包含所有版本資訊")
    void shouldIncludeAllVersions() {
        // Arrange
        var libraryId = randomId();
        var library = createLibrary(libraryId, "spring-boot", "Spring Boot", null, null, null);
        var versions = List.of(
                createVersion(randomId(), libraryId, "3.2.0", true, VersionStatus.ACTIVE),
                createVersion(randomId(), libraryId, "3.1.0", false, VersionStatus.ACTIVE),
                createVersion(randomId(), libraryId, "2.7.0", false, VersionStatus.DEPRECATED)
        );

        when(libraryService.getLibraryById(libraryId)).thenReturn(library);
        when(libraryService.getLibraryVersionsById(libraryId)).thenReturn(versions);

        // Act
        var result = libraryResourceProvider.getLibraryResource(libraryId);

        // Assert
        var resourceContent = (TextResourceContents) result.contents().get(0);
        var content = resourceContent.text();
        assertThat(content).contains("3.2.0");
        assertThat(content).contains("3.1.0");
        assertThat(content).contains("2.7.0");
        assertThat(content).contains("DEPRECATED");
    }

    @Test
    @DisplayName("當函式庫不存在時應拋出例外")
    void shouldThrowExceptionWhenLibraryNotFound() {
        // Arrange
        var libraryId = randomId();
        when(libraryService.getLibraryById(libraryId))
                .thenThrow(LibraryNotFoundException.byId(libraryId));

        // Act & Assert
        assertThatThrownBy(() -> libraryResourceProvider.getLibraryResource(libraryId))
                .isInstanceOf(LibraryNotFoundException.class);
    }

    // 產生隨機 ID 的輔助方法
    private String randomId() {
        return TsidCreator.getTsid().toString();
    }

    private Library createLibrary(String id, String name, String displayName,
                                   String description, SourceType sourceType, String sourceUrl) {
        return new Library(id, name, displayName, description, sourceType, sourceUrl,
                "backend", List.of("java", "framework"), 0L, null, null);
    }

    private LibraryVersion createVersion(String id, String libraryId, String version,
                                          boolean isLatest, VersionStatus status) {
        return new LibraryVersion(id, libraryId, version, isLatest, false, status, null, null, 0L, null, null);
    }
}
