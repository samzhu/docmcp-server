package io.github.samzhu.docmcp.mcp.tool.search;

import io.github.samzhu.docmcp.mcp.dto.SearchResultItem;
import io.github.samzhu.docmcp.mcp.dto.SemanticSearchResult;
import io.github.samzhu.docmcp.service.SearchService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * SemanticSearchTool 單元測試
 */
@DisplayName("SemanticSearchTool")
class SemanticSearchToolTest {

    private SearchService searchService;
    private SemanticSearchTool semanticSearchTool;

    @BeforeEach
    void setUp() {
        searchService = mock(SearchService.class);
        semanticSearchTool = new SemanticSearchTool(searchService);
    }

    @Test
    @DisplayName("should search with default parameters")
    void shouldSearchWithDefaultParameters() {
        // Arrange
        UUID libraryId = UUID.randomUUID();
        String query = "how to configure database connection";
        var expectedResults = List.of(
                SearchResultItem.fromChunk(UUID.randomUUID(), UUID.randomUUID(),
                        "Database Config", "/docs/database.md", "Content...", 0.85, 0)
        );

        when(searchService.semanticSearch(libraryId, null, query, 5, 0.7))
                .thenReturn(expectedResults);

        // Act
        SemanticSearchResult result = semanticSearchTool.semanticSearch(
                libraryId, query, null, null, null);

        // Assert
        assertThat(result.results()).hasSize(1);
        assertThat(result.total()).isEqualTo(1);
        assertThat(result.query()).isEqualTo(query);
        assertThat(result.threshold()).isEqualTo(0.7);
        verify(searchService).semanticSearch(libraryId, null, query, 5, 0.7);
    }

    @Test
    @DisplayName("should search with custom limit")
    void shouldSearchWithCustomLimit() {
        // Arrange
        UUID libraryId = UUID.randomUUID();
        String query = "authentication setup";
        int limit = 10;

        when(searchService.semanticSearch(libraryId, null, query, limit, 0.7))
                .thenReturn(List.of());

        // Act
        semanticSearchTool.semanticSearch(libraryId, query, null, limit, null);

        // Assert
        verify(searchService).semanticSearch(libraryId, null, query, limit, 0.7);
    }

    @Test
    @DisplayName("should search with custom threshold")
    void shouldSearchWithCustomThreshold() {
        // Arrange
        UUID libraryId = UUID.randomUUID();
        String query = "error handling";
        double threshold = 0.9;

        when(searchService.semanticSearch(libraryId, null, query, 5, threshold))
                .thenReturn(List.of());

        // Act
        SemanticSearchResult result = semanticSearchTool.semanticSearch(
                libraryId, query, null, null, threshold);

        // Assert
        assertThat(result.threshold()).isEqualTo(threshold);
        verify(searchService).semanticSearch(libraryId, null, query, 5, threshold);
    }

    @Test
    @DisplayName("should search with specific version")
    void shouldSearchWithSpecificVersion() {
        // Arrange
        UUID libraryId = UUID.randomUUID();
        String query = "migration guide";
        String version = "3.0.0";

        when(searchService.semanticSearch(libraryId, version, query, 5, 0.7))
                .thenReturn(List.of());

        // Act
        semanticSearchTool.semanticSearch(libraryId, query, version, null, null);

        // Assert
        verify(searchService).semanticSearch(libraryId, version, query, 5, 0.7);
    }

    @Test
    @DisplayName("should use default limit when limit is invalid")
    void shouldUseDefaultLimitWhenLimitIsInvalid() {
        // Arrange
        UUID libraryId = UUID.randomUUID();
        String query = "test";

        when(searchService.semanticSearch(libraryId, null, query, 5, 0.7))
                .thenReturn(List.of());

        // Act
        semanticSearchTool.semanticSearch(libraryId, query, null, -1, null);

        // Assert
        verify(searchService).semanticSearch(libraryId, null, query, 5, 0.7);
    }

    @Test
    @DisplayName("should use default threshold when threshold is out of range")
    void shouldUseDefaultThresholdWhenOutOfRange() {
        // Arrange
        UUID libraryId = UUID.randomUUID();
        String query = "test";

        when(searchService.semanticSearch(libraryId, null, query, 5, 0.7))
                .thenReturn(List.of());

        // Act - threshold > 1
        semanticSearchTool.semanticSearch(libraryId, query, null, null, 1.5);
        verify(searchService).semanticSearch(libraryId, null, query, 5, 0.7);

        // Act - threshold < 0
        reset(searchService);
        when(searchService.semanticSearch(libraryId, null, query, 5, 0.7))
                .thenReturn(List.of());
        semanticSearchTool.semanticSearch(libraryId, query, null, null, -0.5);
        verify(searchService).semanticSearch(libraryId, null, query, 5, 0.7);
    }

    @Test
    @DisplayName("should return results with similarity scores")
    void shouldReturnResultsWithSimilarityScores() {
        // Arrange
        UUID libraryId = UUID.randomUUID();
        String query = "dependency injection";
        var expectedResults = List.of(
                SearchResultItem.fromChunk(UUID.randomUUID(), UUID.randomUUID(),
                        "DI Guide", "/docs/di.md", "Content about DI...", 0.92, 0),
                SearchResultItem.fromChunk(UUID.randomUUID(), UUID.randomUUID(),
                        "IoC Container", "/docs/ioc.md", "IoC content...", 0.85, 1)
        );

        when(searchService.semanticSearch(libraryId, null, query, 5, 0.7))
                .thenReturn(expectedResults);

        // Act
        SemanticSearchResult result = semanticSearchTool.semanticSearch(
                libraryId, query, null, null, null);

        // Assert
        assertThat(result.results()).hasSize(2);
        assertThat(result.results().get(0).score()).isEqualTo(0.92);
        assertThat(result.results().get(1).score()).isEqualTo(0.85);
    }

    @Test
    @DisplayName("should return empty results when no matches")
    void shouldReturnEmptyResultsWhenNoMatches() {
        // Arrange
        UUID libraryId = UUID.randomUUID();
        String query = "something very specific that doesn't exist";

        when(searchService.semanticSearch(libraryId, null, query, 5, 0.7))
                .thenReturn(List.of());

        // Act
        SemanticSearchResult result = semanticSearchTool.semanticSearch(
                libraryId, query, null, null, null);

        // Assert
        assertThat(result.results()).isEmpty();
        assertThat(result.total()).isEqualTo(0);
    }
}
