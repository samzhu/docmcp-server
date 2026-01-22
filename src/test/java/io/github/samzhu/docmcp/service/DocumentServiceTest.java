package io.github.samzhu.docmcp.service;

import io.github.samzhu.docmcp.domain.exception.DocumentNotFoundException;
import io.github.samzhu.docmcp.domain.model.CodeExample;
import io.github.samzhu.docmcp.domain.model.Document;
import io.github.samzhu.docmcp.domain.model.DocumentChunk;
import io.github.samzhu.docmcp.domain.model.LibraryVersion;
import io.github.samzhu.docmcp.repository.CodeExampleRepository;
import io.github.samzhu.docmcp.repository.DocumentChunkRepository;
import io.github.samzhu.docmcp.repository.DocumentRepository;
import io.github.samzhu.docmcp.repository.LibraryVersionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.github.f4b6a3.tsid.TsidCreator;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * DocumentService 單元測試
 */
@DisplayName("DocumentService")
class DocumentServiceTest {

    private DocumentRepository documentRepository;
    private DocumentChunkRepository chunkRepository;
    private CodeExampleRepository codeExampleRepository;
    private LibraryVersionRepository versionRepository;
    private DocumentService documentService;

    @BeforeEach
    void setUp() {
        documentRepository = mock(DocumentRepository.class);
        chunkRepository = mock(DocumentChunkRepository.class);
        codeExampleRepository = mock(CodeExampleRepository.class);
        versionRepository = mock(LibraryVersionRepository.class);

        documentService = new DocumentService(documentRepository, chunkRepository,
                codeExampleRepository, versionRepository);
    }

    @Nested
    @DisplayName("getDocumentContent")
    class GetDocumentContentTests {

        @Test
        @DisplayName("should return document content with chunks and examples")
        void shouldReturnDocumentContentWithChunksAndExamples() {
            // Arrange
            String documentId = randomId();
            var document = createDocument(documentId, "Test Doc", "/docs/test.md", "Content");
            var chunk = createChunk(randomId(), documentId, 0, "Chunk content");
            var example = createCodeExample(randomId(), documentId, "java", "code");

            when(documentRepository.findById(documentId)).thenReturn(Optional.of(document));
            when(chunkRepository.findByDocumentIdOrderByChunkIndex(documentId)).thenReturn(List.of(chunk));
            when(codeExampleRepository.findByDocumentId(documentId)).thenReturn(List.of(example));

            // Act
            var result = documentService.getDocumentContent(documentId);

            // Assert
            assertThat(result.document()).isEqualTo(document);
            assertThat(result.chunks()).hasSize(1);
            assertThat(result.codeExamples()).hasSize(1);
        }

        @Test
        @DisplayName("should throw exception when document not found")
        void shouldThrowExceptionWhenDocumentNotFound() {
            // Arrange
            String documentId = randomId();
            when(documentRepository.findById(documentId)).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> documentService.getDocumentContent(documentId))
                    .isInstanceOf(DocumentNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("getDocumentByPath")
    class GetDocumentByPathTests {

        @Test
        @DisplayName("should return document when found")
        void shouldReturnDocumentWhenFound() {
            // Arrange
            String versionId = randomId();
            String path = "/docs/test.md";
            var document = createDocument(randomId(), "Test", path, "Content");

            when(documentRepository.findByVersionIdAndPath(versionId, path))
                    .thenReturn(Optional.of(document));

            // Act
            var result = documentService.getDocumentByPath(versionId, path);

            // Assert
            assertThat(result).isPresent();
            assertThat(result.get().getPath()).isEqualTo(path);
        }

        @Test
        @DisplayName("should return empty when document not found")
        void shouldReturnEmptyWhenDocumentNotFound() {
            // Arrange
            String versionId = randomId();
            when(documentRepository.findByVersionIdAndPath(versionId, "/nonexistent"))
                    .thenReturn(Optional.empty());

            // Act
            var result = documentService.getDocumentByPath(versionId, "/nonexistent");

            // Assert
            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("getCodeExamples")
    class GetCodeExamplesTests {

        @Test
        @DisplayName("should return code examples with specified version")
        void shouldReturnCodeExamplesWithSpecifiedVersion() {
            // Arrange
            String libraryId = randomId();
            String version = "1.0.0";
            var example = createCodeExample(randomId(), randomId(), "java", "code");

            when(codeExampleRepository.findByLibraryAndLanguage(libraryId, version, "java", 10))
                    .thenReturn(List.of(example));

            // Act
            var results = documentService.getCodeExamples(libraryId, version, "java", 10);

            // Assert
            assertThat(results).hasSize(1);
        }

        @Test
        @DisplayName("should use latest version when version is null")
        void shouldUseLatestVersionWhenVersionIsNull() {
            // Arrange
            String libraryId = randomId();
            var latestVersion = createVersion(randomId(), libraryId, "2.0.0", true);
            var example = createCodeExample(randomId(), randomId(), "java", "code");

            when(versionRepository.findLatestByLibraryId(libraryId))
                    .thenReturn(Optional.of(latestVersion));
            when(codeExampleRepository.findByLibraryAndLanguage(libraryId, "2.0.0", null, 10))
                    .thenReturn(List.of(example));

            // Act
            var results = documentService.getCodeExamples(libraryId, null, null, 10);

            // Assert
            assertThat(results).hasSize(1);
        }

        @Test
        @DisplayName("should return empty list when no latest version exists")
        void shouldReturnEmptyListWhenNoLatestVersionExists() {
            // Arrange
            String libraryId = randomId();
            when(versionRepository.findLatestByLibraryId(libraryId))
                    .thenReturn(Optional.empty());
            when(codeExampleRepository.findByLibraryAndLanguage(libraryId, null, null, 10))
                    .thenReturn(List.of());

            // Act
            var results = documentService.getCodeExamples(libraryId, null, null, 10);

            // Assert
            assertThat(results).isEmpty();
        }
    }

    @Nested
    @DisplayName("getAvailableLanguages")
    class GetAvailableLanguagesTests {

        @Test
        @DisplayName("should return available languages")
        void shouldReturnAvailableLanguages() {
            // Arrange
            String libraryId = randomId();
            when(codeExampleRepository.findDistinctLanguagesByLibrary(libraryId, null))
                    .thenReturn(List.of("java", "javascript", "python"));

            // Act
            var languages = documentService.getAvailableLanguages(libraryId, null);

            // Assert
            assertThat(languages).containsExactly("java", "javascript", "python");
        }
    }

    // Helper methods

    /**
     * 生成隨機 TSID
     */
    private String randomId() {
        return TsidCreator.getTsid().toString();
    }

    private Document createDocument(String id, String title, String path, String content) {
        return new Document(id, randomId(), title, path, content, "hash",
                "markdown", Map.of(), null, OffsetDateTime.now(), OffsetDateTime.now());
    }

    private DocumentChunk createChunk(String id, String documentId, int index, String content) {
        var now = OffsetDateTime.now();
        return new DocumentChunk(id, documentId, index, content, null, 100, Map.of(), null, now, now);
    }

    private CodeExample createCodeExample(String id, String documentId, String language, String code) {
        var now = OffsetDateTime.now();
        return new CodeExample(id, documentId, language, code, "Description",
                null, null, Map.of(), null, now, now);
    }

    private LibraryVersion createVersion(String id, String libraryId, String version, boolean isLatest) {
        return new LibraryVersion(id, libraryId, version, isLatest, false, null,
                null, null, null, null, null);
    }
}
