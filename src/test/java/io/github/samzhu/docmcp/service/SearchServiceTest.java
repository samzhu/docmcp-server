package io.github.samzhu.docmcp.service;

import io.github.samzhu.docmcp.domain.model.Document;
import io.github.samzhu.docmcp.domain.model.DocumentChunk;
import io.github.samzhu.docmcp.domain.model.LibraryVersion;
import io.github.samzhu.docmcp.mcp.dto.SearchResultItem;
import io.github.samzhu.docmcp.repository.DocumentChunkRepository;
import io.github.samzhu.docmcp.repository.DocumentRepository;
import io.github.samzhu.docmcp.repository.LibraryVersionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * SearchService 單元測試
 */
@DisplayName("SearchService")
class SearchServiceTest {

    private DocumentRepository documentRepository;
    private DocumentChunkRepository chunkRepository;
    private LibraryVersionRepository versionRepository;
    private EmbeddingService embeddingService;
    private SearchService searchService;

    private UUID libraryId;
    private UUID versionId;

    @BeforeEach
    void setUp() {
        documentRepository = mock(DocumentRepository.class);
        chunkRepository = mock(DocumentChunkRepository.class);
        versionRepository = mock(LibraryVersionRepository.class);
        embeddingService = mock(EmbeddingService.class);

        searchService = new SearchService(documentRepository, chunkRepository,
                versionRepository, embeddingService);

        libraryId = UUID.randomUUID();
        versionId = UUID.randomUUID();
    }

    @Nested
    @DisplayName("fullTextSearch")
    class FullTextSearchTests {

        @Test
        @DisplayName("should return results when query matches documents")
        void shouldReturnResultsWhenQueryMatchesDocuments() {
            // Arrange
            String query = "spring boot";
            var version = createLibraryVersion(versionId, libraryId, "1.0.0", true);
            var document = createDocument(UUID.randomUUID(), versionId, "Getting Started",
                    "/docs/getting-started.md", "Spring Boot makes it easy to create...");

            when(versionRepository.findLatestByLibraryId(libraryId))
                    .thenReturn(Optional.of(version));
            when(documentRepository.fullTextSearch(versionId, query, 10))
                    .thenReturn(List.of(document));

            // Act
            List<SearchResultItem> results = searchService.fullTextSearch(libraryId, null, query, 10);

            // Assert
            assertThat(results).hasSize(1);
            assertThat(results.getFirst().title()).isEqualTo("Getting Started");
            assertThat(results.getFirst().path()).isEqualTo("/docs/getting-started.md");
        }

        @Test
        @DisplayName("should return empty list when query is null")
        void shouldReturnEmptyListWhenQueryIsNull() {
            // Act
            List<SearchResultItem> results = searchService.fullTextSearch(libraryId, null, null, 10);

            // Assert
            assertThat(results).isEmpty();
        }

        @Test
        @DisplayName("should return empty list when query is blank")
        void shouldReturnEmptyListWhenQueryIsBlank() {
            // Act
            List<SearchResultItem> results = searchService.fullTextSearch(libraryId, null, "   ", 10);

            // Assert
            assertThat(results).isEmpty();
        }

        @Test
        @DisplayName("should return empty list when version not found")
        void shouldReturnEmptyListWhenVersionNotFound() {
            // Arrange
            when(versionRepository.findLatestByLibraryId(libraryId))
                    .thenReturn(Optional.empty());

            // Act
            List<SearchResultItem> results = searchService.fullTextSearch(libraryId, null, "spring", 10);

            // Assert
            assertThat(results).isEmpty();
        }

        @Test
        @DisplayName("should use specific version when provided")
        void shouldUseSpecificVersionWhenProvided() {
            // Arrange
            String query = "configuration";
            String versionStr = "2.0.0";
            var version = createLibraryVersion(versionId, libraryId, versionStr, false);
            var document = createDocument(UUID.randomUUID(), versionId, "Configuration",
                    "/docs/config.md", "Configuration options...");

            when(versionRepository.findByLibraryIdAndVersion(libraryId, versionStr))
                    .thenReturn(Optional.of(version));
            when(documentRepository.fullTextSearch(versionId, query, 10))
                    .thenReturn(List.of(document));

            // Act
            List<SearchResultItem> results = searchService.fullTextSearch(libraryId, versionStr, query, 10);

            // Assert
            assertThat(results).hasSize(1);
            assertThat(results.getFirst().title()).isEqualTo("Configuration");
        }

        @Test
        @DisplayName("should truncate long content")
        void shouldTruncateLongContent() {
            // Arrange
            String query = "spring";
            var version = createLibraryVersion(versionId, libraryId, "1.0.0", true);
            String longContent = "A".repeat(600);
            var document = createDocument(UUID.randomUUID(), versionId, "Long Doc",
                    "/docs/long.md", longContent);

            when(versionRepository.findLatestByLibraryId(libraryId))
                    .thenReturn(Optional.of(version));
            when(documentRepository.fullTextSearch(versionId, query, 10))
                    .thenReturn(List.of(document));

            // Act
            List<SearchResultItem> results = searchService.fullTextSearch(libraryId, null, query, 10);

            // Assert
            assertThat(results.getFirst().content()).hasSize(503); // 500 + "..."
            assertThat(results.getFirst().content()).endsWith("...");
        }
    }

    @Nested
    @DisplayName("semanticSearch")
    class SemanticSearchTests {

        @Test
        @DisplayName("should return results when query matches chunks")
        void shouldReturnResultsWhenQueryMatchesChunks() {
            // Arrange
            String query = "how to configure database";
            UUID documentId = UUID.randomUUID();
            UUID chunkId = UUID.randomUUID();

            float[] queryVector = createVector(0.5f);
            float[] chunkVector = createVector(0.5f); // Same vector for 100% similarity

            var version = createLibraryVersion(versionId, libraryId, "1.0.0", true);
            var chunk = createDocumentChunk(chunkId, documentId, 0, "Database configuration...", chunkVector);
            var document = createDocument(documentId, versionId, "Database Config",
                    "/docs/database.md", "Full content...");

            when(versionRepository.findLatestByLibraryId(libraryId))
                    .thenReturn(Optional.of(version));
            when(embeddingService.embed(query)).thenReturn(queryVector);
            when(embeddingService.toVectorString(queryVector)).thenReturn("[0.5,0.5,...]");
            when(chunkRepository.findSimilarChunks(eq(versionId), anyString(), eq(5)))
                    .thenReturn(List.of(chunk));
            when(documentRepository.findAllById(List.of(documentId)))
                    .thenReturn(List.of(document));

            // Act
            List<SearchResultItem> results = searchService.semanticSearch(libraryId, null, query, 5, 0.7);

            // Assert
            assertThat(results).hasSize(1);
            assertThat(results.getFirst().chunkId()).isEqualTo(chunkId);
            assertThat(results.getFirst().chunkIndex()).isEqualTo(0);
            assertThat(results.getFirst().score()).isGreaterThan(0.7);
        }

        @Test
        @DisplayName("should return empty list when query is null")
        void shouldReturnEmptyListWhenQueryIsNull() {
            // Act
            List<SearchResultItem> results = searchService.semanticSearch(libraryId, null, null, 5, 0.7);

            // Assert
            assertThat(results).isEmpty();
        }

        @Test
        @DisplayName("should return empty list when version not found")
        void shouldReturnEmptyListWhenVersionNotFound() {
            // Arrange
            when(versionRepository.findLatestByLibraryId(libraryId))
                    .thenReturn(Optional.empty());

            // Act
            List<SearchResultItem> results = searchService.semanticSearch(libraryId, null, "query", 5, 0.7);

            // Assert
            assertThat(results).isEmpty();
        }

        @Test
        @DisplayName("should filter results below threshold")
        void shouldFilterResultsBelowThreshold() {
            // Arrange
            String query = "query";
            UUID documentId = UUID.randomUUID();

            // Create orthogonal vectors for low similarity
            float[] queryVector = createOrthogonalVector1();
            float[] chunkVector = createOrthogonalVector2();

            var version = createLibraryVersion(versionId, libraryId, "1.0.0", true);
            var chunk = createDocumentChunk(UUID.randomUUID(), documentId, 0, "Content...", chunkVector);
            var document = createDocument(documentId, versionId, "Doc", "/docs/doc.md", "Content");

            when(versionRepository.findLatestByLibraryId(libraryId))
                    .thenReturn(Optional.of(version));
            when(embeddingService.embed(query)).thenReturn(queryVector);
            when(embeddingService.toVectorString(queryVector)).thenReturn("[...]");
            when(chunkRepository.findSimilarChunks(eq(versionId), anyString(), eq(5)))
                    .thenReturn(List.of(chunk));
            when(documentRepository.findAllById(List.of(documentId)))
                    .thenReturn(List.of(document));

            // Act
            List<SearchResultItem> results = searchService.semanticSearch(libraryId, null, query, 5, 0.9);

            // Assert
            assertThat(results).isEmpty(); // Low similarity filtered out
        }
    }

    // Helper methods
    private LibraryVersion createLibraryVersion(UUID id, UUID libraryId, String version, boolean isLatest) {
        return new LibraryVersion(id, libraryId, version, isLatest, null,
                null, null, null, null);
    }

    private Document createDocument(UUID id, UUID versionId, String title, String path, String content) {
        return new Document(id, versionId, title, path, content, null, "markdown",
                Map.of(), OffsetDateTime.now(), OffsetDateTime.now());
    }

    private DocumentChunk createDocumentChunk(UUID id, UUID documentId, int chunkIndex,
                                               String content, float[] embedding) {
        return new DocumentChunk(id, documentId, chunkIndex, content, embedding, 100,
                Map.of(), OffsetDateTime.now());
    }

    private float[] createVector(float value) {
        float[] vector = new float[768];
        for (int i = 0; i < 768; i++) {
            vector[i] = value;
        }
        return vector;
    }

    // Create vectors with low cosine similarity (~0) for testing threshold filtering
    private float[] createOrthogonalVector1() {
        float[] vector = new float[768];
        // First half positive, second half zero
        for (int i = 0; i < 384; i++) {
            vector[i] = 1.0f;
        }
        for (int i = 384; i < 768; i++) {
            vector[i] = 0.0f;
        }
        return vector;
    }

    private float[] createOrthogonalVector2() {
        float[] vector = new float[768];
        // First half zero, second half positive
        for (int i = 0; i < 384; i++) {
            vector[i] = 0.0f;
        }
        for (int i = 384; i < 768; i++) {
            vector[i] = 1.0f;
        }
        return vector;
    }
}
