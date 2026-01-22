package io.github.samzhu.docmcp.service;

import io.github.samzhu.docmcp.domain.model.Document;
import io.github.samzhu.docmcp.domain.model.LibraryVersion;
import io.github.samzhu.docmcp.mcp.dto.SearchResultItem;
import io.github.samzhu.docmcp.repository.DocumentRepository;
import io.github.samzhu.docmcp.repository.LibraryVersionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;

import com.github.f4b6a3.tsid.TsidCreator;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static io.github.samzhu.docmcp.infrastructure.vectorstore.DocumentChunkVectorStore.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * SearchService 單元測試
 * <p>
 * 測試 SearchService 使用 VectorStore 進行語意搜尋的功能。
 * </p>
 */
@DisplayName("SearchService")
class SearchServiceTest {

    private DocumentRepository documentRepository;
    private LibraryVersionRepository versionRepository;
    private VectorStore vectorStore;
    private SearchService searchService;

    private String libraryId;
    private String versionId;

    @BeforeEach
    void setUp() {
        documentRepository = mock(DocumentRepository.class);
        versionRepository = mock(LibraryVersionRepository.class);
        vectorStore = mock(VectorStore.class);

        searchService = new SearchService(documentRepository, versionRepository, vectorStore);

        libraryId = randomId();
        versionId = randomId();
    }

    /**
     * 生成隨機 TSID
     */
    private String randomId() {
        return TsidCreator.getTsid().toString();
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
            var document = createDocument(randomId(), versionId, "Getting Started",
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
            var document = createDocument(randomId(), versionId, "Configuration",
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
            var document = createDocument(randomId(), versionId, "Long Doc",
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
        @DisplayName("should return results when VectorStore returns matching documents")
        void shouldReturnResultsWhenVectorStoreReturnsMatchingDocuments() {
            // Arrange
            String query = "how to configure database";
            String documentId = randomId();
            String chunkId = randomId();

            var version = createLibraryVersion(versionId, libraryId, "1.0.0", true);
            var document = createDocument(documentId, versionId, "Database Config",
                    "/docs/database.md", "Full content...");

            // 建立模擬的 Spring AI Document（Spring AI 2.0 使用建構子）
            var aiDoc = new org.springframework.ai.document.Document(
                    chunkId,
                    "Database configuration...",
                    Map.of(
                            METADATA_VERSION_ID, versionId,
                            METADATA_DOCUMENT_ID, documentId,
                            METADATA_CHUNK_INDEX, 0,
                            "score", 0.85
                    )
            );

            when(versionRepository.findLatestByLibraryId(libraryId))
                    .thenReturn(Optional.of(version));
            when(vectorStore.similaritySearch(any(SearchRequest.class)))
                    .thenReturn(List.of(aiDoc));
            when(documentRepository.findAllById(List.of(documentId)))
                    .thenReturn(List.of(document));

            // Act
            List<SearchResultItem> results = searchService.semanticSearch(libraryId, null, query, 5, 0.7);

            // Assert
            assertThat(results).hasSize(1);
            assertThat(results.getFirst().chunkId()).isEqualTo(chunkId);
            assertThat(results.getFirst().chunkIndex()).isEqualTo(0);
            assertThat(results.getFirst().score()).isEqualTo(0.85);
            assertThat(results.getFirst().title()).isEqualTo("Database Config");
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
        @DisplayName("should return empty list when query is blank")
        void shouldReturnEmptyListWhenQueryIsBlank() {
            // Act
            List<SearchResultItem> results = searchService.semanticSearch(libraryId, null, "   ", 5, 0.7);

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
        @DisplayName("should return empty list when VectorStore returns no results")
        void shouldReturnEmptyListWhenVectorStoreReturnsNoResults() {
            // Arrange
            var version = createLibraryVersion(versionId, libraryId, "1.0.0", true);

            when(versionRepository.findLatestByLibraryId(libraryId))
                    .thenReturn(Optional.of(version));
            when(vectorStore.similaritySearch(any(SearchRequest.class)))
                    .thenReturn(List.of());

            // Act
            List<SearchResultItem> results = searchService.semanticSearch(libraryId, null, "query", 5, 0.7);

            // Assert
            assertThat(results).isEmpty();
        }

        @Test
        @DisplayName("should use specific version when provided - semantic")
        void shouldUseSpecificVersionWhenProvidedSemantic() {
            // Arrange
            String query = "configuration";
            String versionStr = "2.0.0";
            String documentId = randomId();

            var version = createLibraryVersion(versionId, libraryId, versionStr, false);
            var document = createDocument(documentId, versionId, "Configuration",
                    "/docs/config.md", "Configuration options...");

            var aiDoc = new org.springframework.ai.document.Document(
                    randomId(),
                    "Configuration content...",
                    Map.of(
                            METADATA_VERSION_ID, versionId,
                            METADATA_DOCUMENT_ID, documentId,
                            METADATA_CHUNK_INDEX, 0,
                            "score", 0.9
                    )
            );

            when(versionRepository.findByLibraryIdAndVersion(libraryId, versionStr))
                    .thenReturn(Optional.of(version));
            when(vectorStore.similaritySearch(any(SearchRequest.class)))
                    .thenReturn(List.of(aiDoc));
            when(documentRepository.findAllById(List.of(documentId)))
                    .thenReturn(List.of(document));

            // Act
            List<SearchResultItem> results = searchService.semanticSearch(libraryId, versionStr, query, 5, 0.7);

            // Assert
            assertThat(results).hasSize(1);
            assertThat(results.getFirst().title()).isEqualTo("Configuration");
        }

        @Test
        @DisplayName("should skip documents with missing metadata")
        void shouldSkipDocumentsWithMissingMetadata() {
            // Arrange
            var version = createLibraryVersion(versionId, libraryId, "1.0.0", true);

            // 建立一個缺少 documentId 的 AI Document
            var aiDocMissingMetadata = new org.springframework.ai.document.Document(
                    randomId(),
                    "Some content...",
                    Map.of(METADATA_VERSION_ID, versionId)
                    // 缺少 METADATA_DOCUMENT_ID
            );

            when(versionRepository.findLatestByLibraryId(libraryId))
                    .thenReturn(Optional.of(version));
            when(vectorStore.similaritySearch(any(SearchRequest.class)))
                    .thenReturn(List.of(aiDocMissingMetadata));

            // Act
            List<SearchResultItem> results = searchService.semanticSearch(libraryId, null, "query", 5, 0.7);

            // Assert
            assertThat(results).isEmpty();
        }
    }

    @Nested
    @DisplayName("hybridSearch")
    class HybridSearchTests {

        @Test
        @DisplayName("should return empty list when query is null")
        void shouldReturnEmptyListWhenQueryIsNull() {
            // Act
            List<SearchResultItem> results = searchService.hybridSearch(libraryId, null, null, 10, 0.3, 0.5);

            // Assert
            assertThat(results).isEmpty();
        }

        @Test
        @DisplayName("should return empty list when query is blank")
        void shouldReturnEmptyListWhenQueryIsBlank() {
            // Act
            List<SearchResultItem> results = searchService.hybridSearch(libraryId, null, "   ", 10, 0.3, 0.5);

            // Assert
            assertThat(results).isEmpty();
        }

        @Test
        @DisplayName("should return keyword results only when semantic search returns empty")
        void shouldReturnKeywordResultsOnlyWhenSemanticSearchReturnsEmpty() {
            // Arrange
            String query = "spring boot";
            var version = createLibraryVersion(versionId, libraryId, "1.0.0", true);
            var document = createDocument(randomId(), versionId, "Getting Started",
                    "/docs/getting-started.md", "Spring Boot makes it easy to create...");

            when(versionRepository.findLatestByLibraryId(libraryId))
                    .thenReturn(Optional.of(version));
            when(documentRepository.fullTextSearch(eq(versionId), eq(query), anyInt()))
                    .thenReturn(List.of(document));
            when(vectorStore.similaritySearch(any(SearchRequest.class)))
                    .thenReturn(List.of());

            // Act
            List<SearchResultItem> results = searchService.hybridSearch(libraryId, null, query, 10, 0.3, 0.5);

            // Assert
            assertThat(results).hasSize(1);
            assertThat(results.getFirst().title()).isEqualTo("Getting Started");
        }

        @Test
        @DisplayName("should return semantic results only when keyword search returns empty")
        void shouldReturnSemanticResultsOnlyWhenKeywordSearchReturnsEmpty() {
            // Arrange
            String query = "how to configure database";
            String documentId = randomId();
            String chunkId = randomId();

            var version = createLibraryVersion(versionId, libraryId, "1.0.0", true);
            var document = createDocument(documentId, versionId, "Database Config",
                    "/docs/database.md", "Full content...");

            var aiDoc = new org.springframework.ai.document.Document(
                    chunkId,
                    "Database configuration...",
                    Map.of(
                            METADATA_VERSION_ID, versionId,
                            METADATA_DOCUMENT_ID, documentId,
                            METADATA_CHUNK_INDEX, 0,
                            "score", 0.85
                    )
            );

            when(versionRepository.findLatestByLibraryId(libraryId))
                    .thenReturn(Optional.of(version));
            when(documentRepository.fullTextSearch(eq(versionId), eq(query), anyInt()))
                    .thenReturn(List.of());
            when(vectorStore.similaritySearch(any(SearchRequest.class)))
                    .thenReturn(List.of(aiDoc));
            when(documentRepository.findAllById(List.of(documentId)))
                    .thenReturn(List.of(document));

            // Act
            List<SearchResultItem> results = searchService.hybridSearch(libraryId, null, query, 10, 0.3, 0.5);

            // Assert
            assertThat(results).hasSize(1);
            assertThat(results.getFirst().title()).isEqualTo("Database Config");
            assertThat(results.getFirst().chunkId()).isEqualTo(chunkId);
        }

        @Test
        @DisplayName("should fuse results from both searches using RRF algorithm")
        void shouldFuseResultsFromBothSearchesUsingRRF() {
            // Arrange
            String query = "spring configuration";
            String docId1 = randomId();
            String docId2 = randomId();
            String chunkId1 = randomId();
            String chunkId2 = randomId();

            var version = createLibraryVersion(versionId, libraryId, "1.0.0", true);
            var document1 = createDocument(docId1, versionId, "Spring Config",
                    "/docs/spring-config.md", "Spring configuration...");
            var document2 = createDocument(docId2, versionId, "Database Config",
                    "/docs/database.md", "Database configuration...");

            // 關鍵字搜尋結果：doc1 排第一，doc2 排第二
            when(versionRepository.findLatestByLibraryId(libraryId))
                    .thenReturn(Optional.of(version));
            when(documentRepository.fullTextSearch(eq(versionId), eq(query), anyInt()))
                    .thenReturn(List.of(document1, document2));

            // 語意搜尋結果：doc2 排第一，doc1 排第二
            var aiDoc1 = new org.springframework.ai.document.Document(
                    chunkId1,
                    "Spring configuration...",
                    Map.of(
                            METADATA_VERSION_ID, versionId,
                            METADATA_DOCUMENT_ID, docId1,
                            METADATA_CHUNK_INDEX, 0,
                            "score", 0.75
                    )
            );
            var aiDoc2 = new org.springframework.ai.document.Document(
                    chunkId2,
                    "Database configuration...",
                    Map.of(
                            METADATA_VERSION_ID, versionId,
                            METADATA_DOCUMENT_ID, docId2,
                            METADATA_CHUNK_INDEX, 0,
                            "score", 0.85
                    )
            );

            when(vectorStore.similaritySearch(any(SearchRequest.class)))
                    .thenReturn(List.of(aiDoc2, aiDoc1));  // doc2 排第一
            when(documentRepository.findAllById(anyList()))
                    .thenReturn(List.of(document1, document2));

            // Act
            List<SearchResultItem> results = searchService.hybridSearch(libraryId, null, query, 10, 0.3, 0.5);

            // Assert
            assertThat(results).hasSize(4);  // 2 個文件結果 + 2 個 chunk 結果
            // RRF 融合後，出現在兩種搜尋的結果應該有更高的分數
            // 分數應該已被正規化為 0-1 範圍
            assertThat(results.getFirst().score()).isGreaterThan(0);
            assertThat(results.getFirst().score()).isLessThanOrEqualTo(1.0);
        }

        @Test
        @DisplayName("should respect limit parameter")
        void shouldRespectLimitParameter() {
            // Arrange
            String query = "spring";
            var version = createLibraryVersion(versionId, libraryId, "1.0.0", true);

            // 建立多個文件
            var documents = List.of(
                    createDocument(randomId(), versionId, "Doc 1", "/docs/1.md", "Content 1"),
                    createDocument(randomId(), versionId, "Doc 2", "/docs/2.md", "Content 2"),
                    createDocument(randomId(), versionId, "Doc 3", "/docs/3.md", "Content 3"),
                    createDocument(randomId(), versionId, "Doc 4", "/docs/4.md", "Content 4"),
                    createDocument(randomId(), versionId, "Doc 5", "/docs/5.md", "Content 5")
            );

            when(versionRepository.findLatestByLibraryId(libraryId))
                    .thenReturn(Optional.of(version));
            when(documentRepository.fullTextSearch(eq(versionId), eq(query), anyInt()))
                    .thenReturn(documents);
            when(vectorStore.similaritySearch(any(SearchRequest.class)))
                    .thenReturn(List.of());

            // Act
            List<SearchResultItem> results = searchService.hybridSearch(libraryId, null, query, 3, 0.3, 0.5);

            // Assert
            assertThat(results).hasSize(3);
        }
    }

    // Helper methods

    /**
     * 建立測試用的 LibraryVersion
     */
    private LibraryVersion createLibraryVersion(String id, String libraryId, String version, boolean isLatest) {
        return new LibraryVersion(id, libraryId, version, isLatest, false, null,
                null, null, null, null, null);
    }

    /**
     * 建立測試用的 Document
     */
    private Document createDocument(String id, String versionId, String title, String path, String content) {
        return new Document(id, versionId, title, path, content, null, "markdown",
                Map.of(), null, OffsetDateTime.now(), OffsetDateTime.now());
    }
}
