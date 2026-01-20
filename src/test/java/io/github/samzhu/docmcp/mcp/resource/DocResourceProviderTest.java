package io.github.samzhu.docmcp.mcp.resource;

import io.github.samzhu.docmcp.domain.enums.VersionStatus;
import io.github.samzhu.docmcp.domain.exception.LibraryNotFoundException;
import io.github.samzhu.docmcp.domain.model.Document;
import io.github.samzhu.docmcp.domain.model.Library;
import io.github.samzhu.docmcp.domain.model.LibraryVersion;
import io.github.samzhu.docmcp.repository.DocumentRepository;
import io.github.samzhu.docmcp.service.LibraryService;
import io.modelcontextprotocol.spec.McpSchema.TextResourceContents;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

/**
 * DocResourceProvider 單元測試
 */
@ExtendWith(MockitoExtension.class)
class DocResourceProviderTest {

    @Mock
    private LibraryService libraryService;

    @Mock
    private DocumentRepository documentRepository;

    private DocResourceProvider docResourceProvider;

    @BeforeEach
    void setUp() {
        docResourceProvider = new DocResourceProvider(libraryService, documentRepository);
    }

    @Test
    @DisplayName("應回傳指定版本的文件內容")
    void shouldReturnDocumentContent() {
        // Arrange
        var libraryId = UUID.randomUUID();
        var versionId = UUID.randomUUID();
        var docId = UUID.randomUUID();
        var library = createLibrary(libraryId, "spring-boot", "Spring Boot");
        var version = createVersion(versionId, libraryId, "3.2.0", true);
        var resolvedLibrary = new LibraryService.ResolvedLibrary(library, version, "3.2.0");
        var document = createDocument(docId, versionId, "Getting Started",
                "docs/getting-started.md", "# Getting Started\n\nWelcome to Spring Boot!");

        when(libraryService.resolveLibrary("spring-boot", "3.2.0")).thenReturn(resolvedLibrary);
        when(documentRepository.findByVersionIdAndPath(versionId, "docs/getting-started.md"))
                .thenReturn(Optional.of(document));

        // Act
        var result = docResourceProvider.getDocResource("spring-boot", "3.2.0", "docs/getting-started.md");

        // Assert
        assertThat(result.contents()).hasSize(1);
        var content = (TextResourceContents) result.contents().get(0);
        assertThat(content.uri()).isEqualTo("docs://spring-boot/3.2.0/docs/getting-started.md");
        assertThat(content.mimeType()).isEqualTo("text/markdown");
        assertThat(content.text()).isEqualTo("# Getting Started\n\nWelcome to Spring Boot!");
    }

    @Test
    @DisplayName("應回傳最新版本的文件內容")
    void shouldReturnLatestVersionDocumentContent() {
        // Arrange
        var libraryId = UUID.randomUUID();
        var versionId = UUID.randomUUID();
        var docId = UUID.randomUUID();
        var library = createLibrary(libraryId, "react", "React");
        var version = createVersion(versionId, libraryId, "18.2.0", true);
        var resolvedLibrary = new LibraryService.ResolvedLibrary(library, version, "18.2.0");
        var document = createDocument(docId, versionId, "Hooks", "docs/hooks.md", "# React Hooks");

        when(libraryService.resolveLibrary("react", null)).thenReturn(resolvedLibrary);
        when(documentRepository.findByVersionIdAndPath(versionId, "docs/hooks.md"))
                .thenReturn(Optional.of(document));

        // Act
        var result = docResourceProvider.getLatestDocResource("react", "docs/hooks.md");

        // Assert
        assertThat(result.contents()).hasSize(1);
        var content = (TextResourceContents) result.contents().get(0);
        assertThat(content.uri()).isEqualTo("docs://react/latest/docs/hooks.md");
    }

    @Test
    @DisplayName("當文件不存在時應拋出例外")
    void shouldThrowExceptionWhenDocumentNotFound() {
        // Arrange
        var libraryId = UUID.randomUUID();
        var versionId = UUID.randomUUID();
        var library = createLibrary(libraryId, "spring-boot", "Spring Boot");
        var version = createVersion(versionId, libraryId, "3.2.0", true);
        var resolvedLibrary = new LibraryService.ResolvedLibrary(library, version, "3.2.0");

        when(libraryService.resolveLibrary("spring-boot", "3.2.0")).thenReturn(resolvedLibrary);
        when(documentRepository.findByVersionIdAndPath(versionId, "docs/non-existent.md"))
                .thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> docResourceProvider.getDocResource(
                "spring-boot", "3.2.0", "docs/non-existent.md"))
                .isInstanceOf(LibraryNotFoundException.class)
                .hasMessageContaining("文件不存在");
    }

    @Test
    @DisplayName("當函式庫不存在時應拋出例外")
    void shouldThrowExceptionWhenLibraryNotFound() {
        // Arrange
        when(libraryService.resolveLibrary("non-existent", "1.0.0"))
                .thenThrow(LibraryNotFoundException.byName("non-existent"));

        // Act & Assert
        assertThatThrownBy(() -> docResourceProvider.getDocResource(
                "non-existent", "1.0.0", "docs/index.md"))
                .isInstanceOf(LibraryNotFoundException.class);
    }

    private Library createLibrary(UUID id, String name, String displayName) {
        return new Library(id, name, displayName, null, null, null, null, null, null, null);
    }

    private LibraryVersion createVersion(UUID id, UUID libraryId, String version, boolean isLatest) {
        return new LibraryVersion(id, libraryId, version, isLatest, false, VersionStatus.ACTIVE, null, null, null, null);
    }

    private Document createDocument(UUID id, UUID versionId, String title, String path, String content) {
        return new Document(id, versionId, title, path, content, null, "markdown", null, null, null);
    }
}
