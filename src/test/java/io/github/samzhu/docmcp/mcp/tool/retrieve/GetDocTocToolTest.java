package io.github.samzhu.docmcp.mcp.tool.retrieve;

import io.github.samzhu.docmcp.domain.enums.VersionStatus;
import io.github.samzhu.docmcp.domain.exception.LibraryNotFoundException;
import io.github.samzhu.docmcp.domain.model.Document;
import io.github.samzhu.docmcp.domain.model.Library;
import io.github.samzhu.docmcp.domain.model.LibraryVersion;
import io.github.samzhu.docmcp.repository.DocumentRepository;
import io.github.samzhu.docmcp.service.LibraryService;
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
 * GetDocTocTool 單元測試
 */
@ExtendWith(MockitoExtension.class)
class GetDocTocToolTest {

    @Mock
    private LibraryService libraryService;

    @Mock
    private DocumentRepository documentRepository;

    private GetDocTocTool getDocTocTool;

    @BeforeEach
    void setUp() {
        getDocTocTool = new GetDocTocTool(libraryService, documentRepository);
    }

    @Test
    @DisplayName("應回傳文件目錄結構")
    void shouldReturnDocumentToc() {
        // Arrange
        var libraryId = UUID.randomUUID();
        var versionId = UUID.randomUUID();
        var library = createLibrary(libraryId, "spring-boot", "Spring Boot");
        var version = createVersion(versionId, libraryId, "3.2.0", true);
        var resolvedLibrary = new LibraryService.ResolvedLibrary(library, version, "3.2.0");

        var documents = List.of(
                createDocument(versionId, "Getting Started", "docs/getting-started.md"),
                createDocument(versionId, "Configuration", "docs/configuration/overview.md"),
                createDocument(versionId, "Data Source", "docs/configuration/datasource.md"),
                createDocument(versionId, "Web MVC", "docs/web/mvc.md")
        );

        when(libraryService.resolveLibrary("spring-boot", null)).thenReturn(resolvedLibrary);
        when(documentRepository.findByVersionIdOrderByPathAsc(versionId)).thenReturn(documents);

        // Act
        var result = getDocTocTool.getDocToc("spring-boot", null, null);

        // Assert
        assertThat(result.libraryId()).isEqualTo(libraryId.toString());
        assertThat(result.libraryName()).isEqualTo("spring-boot");
        assertThat(result.version()).isEqualTo("3.2.0");
        assertThat(result.totalDocs()).isEqualTo(4);
        assertThat(result.entries()).isNotEmpty();
    }

    @Test
    @DisplayName("應支援指定版本查詢目錄")
    void shouldSupportVersionSpecificToc() {
        // Arrange
        var libraryId = UUID.randomUUID();
        var versionId = UUID.randomUUID();
        var library = createLibrary(libraryId, "react", "React");
        var version = createVersion(versionId, libraryId, "17.0.0", false);
        var resolvedLibrary = new LibraryService.ResolvedLibrary(library, version, "17.0.0");

        var documents = List.of(
                createDocument(versionId, "Introduction", "docs/intro.md"),
                createDocument(versionId, "Hooks", "docs/hooks/overview.md")
        );

        when(libraryService.resolveLibrary("react", "17.0.0")).thenReturn(resolvedLibrary);
        when(documentRepository.findByVersionIdOrderByPathAsc(versionId)).thenReturn(documents);

        // Act
        var result = getDocTocTool.getDocToc("react", "17.0.0", null);

        // Assert
        assertThat(result.version()).isEqualTo("17.0.0");
        assertThat(result.totalDocs()).isEqualTo(2);
    }

    @Test
    @DisplayName("應限制目錄深度")
    void shouldLimitTocDepth() {
        // Arrange
        var libraryId = UUID.randomUUID();
        var versionId = UUID.randomUUID();
        var library = createLibrary(libraryId, "spring-boot", "Spring Boot");
        var version = createVersion(versionId, libraryId, "3.2.0", true);
        var resolvedLibrary = new LibraryService.ResolvedLibrary(library, version, "3.2.0");

        var documents = List.of(
                createDocument(versionId, "Level 0", "docs/index.md"),
                createDocument(versionId, "Level 1", "docs/level1/index.md"),
                createDocument(versionId, "Level 2", "docs/level1/level2/index.md"),
                createDocument(versionId, "Level 3", "docs/level1/level2/level3/index.md")
        );

        when(libraryService.resolveLibrary("spring-boot", null)).thenReturn(resolvedLibrary);
        when(documentRepository.findByVersionIdOrderByPathAsc(versionId)).thenReturn(documents);

        // Act - maxDepth 為 2
        var result = getDocTocTool.getDocToc("spring-boot", null, 2);

        // Assert
        assertThat(result.entries()).isNotEmpty();
        // 驗證深度限制
        assertThat(result.entries().stream()
                .flatMap(e -> getAllEntries(e).stream())
                .allMatch(e -> e.depth() <= 2)).isTrue();
    }

    @Test
    @DisplayName("當沒有文件時應回傳空目錄")
    void shouldReturnEmptyTocWhenNoDocuments() {
        // Arrange
        var libraryId = UUID.randomUUID();
        var versionId = UUID.randomUUID();
        var library = createLibrary(libraryId, "new-lib", "New Library");
        var version = createVersion(versionId, libraryId, "1.0.0", true);
        var resolvedLibrary = new LibraryService.ResolvedLibrary(library, version, "1.0.0");

        when(libraryService.resolveLibrary("new-lib", null)).thenReturn(resolvedLibrary);
        when(documentRepository.findByVersionIdOrderByPathAsc(versionId)).thenReturn(List.of());

        // Act
        var result = getDocTocTool.getDocToc("new-lib", null, null);

        // Assert
        assertThat(result.entries()).isEmpty();
        assertThat(result.totalDocs()).isEqualTo(0);
    }

    @Test
    @DisplayName("當函式庫不存在時應拋出例外")
    void shouldThrowExceptionWhenLibraryNotFound() {
        // Arrange
        when(libraryService.resolveLibrary("non-existent", null))
                .thenThrow(LibraryNotFoundException.byName("non-existent"));

        // Act & Assert
        assertThatThrownBy(() -> getDocTocTool.getDocToc("non-existent", null, null))
                .isInstanceOf(LibraryNotFoundException.class);
    }

    private Library createLibrary(UUID id, String name, String displayName) {
        return new Library(id, name, displayName, null, null, null, null, null, null, null);
    }

    private LibraryVersion createVersion(UUID id, UUID libraryId, String version, boolean isLatest) {
        return new LibraryVersion(id, libraryId, version, isLatest, false, VersionStatus.ACTIVE, null, null, null, null);
    }

    private Document createDocument(UUID versionId, String title, String path) {
        return new Document(UUID.randomUUID(), versionId, title, path, "content", null, "markdown", null, null, null);
    }

    /**
     * 遞迴取得所有條目（包含子條目）
     */
    private List<io.github.samzhu.docmcp.mcp.dto.GetDocTocResult.TocEntry> getAllEntries(
            io.github.samzhu.docmcp.mcp.dto.GetDocTocResult.TocEntry entry) {
        var result = new java.util.ArrayList<io.github.samzhu.docmcp.mcp.dto.GetDocTocResult.TocEntry>();
        result.add(entry);
        if (entry.children() != null) {
            for (var child : entry.children()) {
                result.addAll(getAllEntries(child));
            }
        }
        return result;
    }
}
