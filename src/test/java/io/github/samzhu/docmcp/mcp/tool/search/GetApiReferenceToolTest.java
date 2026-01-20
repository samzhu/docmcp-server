package io.github.samzhu.docmcp.mcp.tool.search;

import io.github.samzhu.docmcp.domain.enums.VersionStatus;
import io.github.samzhu.docmcp.domain.exception.LibraryNotFoundException;
import io.github.samzhu.docmcp.domain.model.Library;
import io.github.samzhu.docmcp.domain.model.LibraryVersion;
import io.github.samzhu.docmcp.mcp.dto.SearchResultItem;
import io.github.samzhu.docmcp.service.LibraryService;
import io.github.samzhu.docmcp.service.SearchService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

/**
 * GetApiReferenceTool 單元測試
 */
@ExtendWith(MockitoExtension.class)
class GetApiReferenceToolTest {

    @Mock
    private LibraryService libraryService;

    @Mock
    private SearchService searchService;

    private GetApiReferenceTool getApiReferenceTool;

    @BeforeEach
    void setUp() {
        getApiReferenceTool = new GetApiReferenceTool(libraryService, searchService);
    }

    @Test
    @DisplayName("應搜尋並回傳 API 參考文件")
    void shouldSearchAndReturnApiReference() {
        // Arrange
        var libraryId = UUID.randomUUID();
        var versionId = UUID.randomUUID();
        var library = createLibrary(libraryId, "spring-boot", "Spring Boot");
        var version = createVersion(versionId, libraryId, "3.2.0", true);
        var resolvedLibrary = new LibraryService.ResolvedLibrary(library, version, "3.2.0");

        var docId = UUID.randomUUID();
        var searchResults = List.of(
                SearchResultItem.fromDocument(
                        docId,
                        "RestController Annotation",
                        "docs/api/RestController.md",
                        "The @RestController annotation is used to create RESTful web services...",
                        0.95
                )
        );

        when(libraryService.resolveLibrary("spring-boot", null)).thenReturn(resolvedLibrary);
        when(searchService.fullTextSearch(libraryId, "3.2.0", "RestController", 10))
                .thenReturn(searchResults);

        // Act
        var result = getApiReferenceTool.getApiReference("spring-boot", "RestController", null);

        // Assert
        assertThat(result.libraryId()).isEqualTo(libraryId.toString());
        assertThat(result.libraryName()).isEqualTo("spring-boot");
        assertThat(result.version()).isEqualTo("3.2.0");
        assertThat(result.apiName()).isEqualTo("RestController");
        assertThat(result.found()).isTrue();
        assertThat(result.results()).hasSize(1);
        assertThat(result.results().get(0).title()).isEqualTo("RestController Annotation");
    }

    @Test
    @DisplayName("應支援指定版本搜尋 API 參考")
    void shouldSupportVersionSpecificSearch() {
        // Arrange
        var libraryId = UUID.randomUUID();
        var versionId = UUID.randomUUID();
        var library = createLibrary(libraryId, "react", "React");
        var version = createVersion(versionId, libraryId, "17.0.0", false);
        var resolvedLibrary = new LibraryService.ResolvedLibrary(library, version, "17.0.0");

        var docId = UUID.randomUUID();
        var searchResults = List.of(
                SearchResultItem.fromDocument(
                        docId,
                        "useState Hook",
                        "docs/hooks/useState.md",
                        "The useState hook is used for managing state in functional components...",
                        0.90
                )
        );

        when(libraryService.resolveLibrary("react", "17.0.0")).thenReturn(resolvedLibrary);
        when(searchService.fullTextSearch(libraryId, "17.0.0", "useState", 10))
                .thenReturn(searchResults);

        // Act
        var result = getApiReferenceTool.getApiReference("react", "useState", "17.0.0");

        // Assert
        assertThat(result.version()).isEqualTo("17.0.0");
        assertThat(result.found()).isTrue();
        assertThat(result.results()).hasSize(1);
    }

    @Test
    @DisplayName("當找不到 API 參考時應標記 found 為 false")
    void shouldIndicateNotFoundWhenNoResults() {
        // Arrange
        var libraryId = UUID.randomUUID();
        var versionId = UUID.randomUUID();
        var library = createLibrary(libraryId, "spring-boot", "Spring Boot");
        var version = createVersion(versionId, libraryId, "3.2.0", true);
        var resolvedLibrary = new LibraryService.ResolvedLibrary(library, version, "3.2.0");

        when(libraryService.resolveLibrary("spring-boot", null)).thenReturn(resolvedLibrary);
        when(searchService.fullTextSearch(libraryId, "3.2.0", "NonExistentApi", 10))
                .thenReturn(List.of());

        // Act
        var result = getApiReferenceTool.getApiReference("spring-boot", "NonExistentApi", null);

        // Assert
        assertThat(result.found()).isFalse();
        assertThat(result.results()).isEmpty();
    }

    @Test
    @DisplayName("應回傳多個相關的 API 參考文件")
    void shouldReturnMultipleRelatedResults() {
        // Arrange
        var libraryId = UUID.randomUUID();
        var versionId = UUID.randomUUID();
        var library = createLibrary(libraryId, "spring-boot", "Spring Boot");
        var version = createVersion(versionId, libraryId, "3.2.0", true);
        var resolvedLibrary = new LibraryService.ResolvedLibrary(library, version, "3.2.0");

        var searchResults = List.of(
                SearchResultItem.fromDocument(UUID.randomUUID(), "RequestMapping",
                        "docs/web/RequestMapping.md", "RequestMapping annotation...", 0.95),
                SearchResultItem.fromDocument(UUID.randomUUID(), "GetMapping",
                        "docs/web/GetMapping.md", "GetMapping is a shortcut for RequestMapping...", 0.85),
                SearchResultItem.fromDocument(UUID.randomUUID(), "PostMapping",
                        "docs/web/PostMapping.md", "PostMapping is a shortcut for RequestMapping...", 0.80)
        );

        when(libraryService.resolveLibrary("spring-boot", null)).thenReturn(resolvedLibrary);
        when(searchService.fullTextSearch(libraryId, "3.2.0", "RequestMapping", 10))
                .thenReturn(searchResults);

        // Act
        var result = getApiReferenceTool.getApiReference("spring-boot", "RequestMapping", null);

        // Assert
        assertThat(result.found()).isTrue();
        assertThat(result.results()).hasSize(3);
    }

    @Test
    @DisplayName("當函式庫不存在時應拋出例外")
    void shouldThrowExceptionWhenLibraryNotFound() {
        // Arrange
        when(libraryService.resolveLibrary("non-existent", null))
                .thenThrow(LibraryNotFoundException.byName("non-existent"));

        // Act & Assert
        assertThatThrownBy(() -> getApiReferenceTool.getApiReference("non-existent", "SomeApi", null))
                .isInstanceOf(LibraryNotFoundException.class);
    }

    private Library createLibrary(UUID id, String name, String displayName) {
        return new Library(id, name, displayName, null, null, null, null, null, null, null);
    }

    private LibraryVersion createVersion(UUID id, UUID libraryId, String version, boolean isLatest) {
        return new LibraryVersion(id, libraryId, version, isLatest, false, VersionStatus.ACTIVE, null, null, null, null);
    }
}
