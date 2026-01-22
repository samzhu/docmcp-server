package io.github.samzhu.docmcp.mcp.tool.search;

import io.github.samzhu.docmcp.mcp.dto.SearchDocsResult;
import io.github.samzhu.docmcp.mcp.dto.SearchResultItem;
import io.github.samzhu.docmcp.service.SearchService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import com.github.f4b6a3.tsid.TsidCreator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * SearchDocsTool 單元測試
 */
@DisplayName("SearchDocsTool")
class SearchDocsToolTest {

    private SearchService searchService;
    private SearchDocsTool searchDocsTool;

    @BeforeEach
    void setUp() {
        searchService = mock(SearchService.class);
        searchDocsTool = new SearchDocsTool(searchService);
    }

    @Test
    @DisplayName("should search docs with default limit")
    void shouldSearchDocsWithDefaultLimit() {
        // Arrange
        String libraryId = randomId();
        String query = "spring boot";
        var expectedResults = List.of(
                SearchResultItem.fromDocument(randomId(), "Getting Started",
                        "/docs/getting-started.md", "Content...", 1.0)
        );

        when(searchService.fullTextSearch(libraryId, null, query, 10))
                .thenReturn(expectedResults);

        // Act
        SearchDocsResult result = searchDocsTool.searchDocs(libraryId, query, null, null);

        // Assert
        assertThat(result.results()).hasSize(1);
        assertThat(result.total()).isEqualTo(1);
        assertThat(result.query()).isEqualTo(query);
        verify(searchService).fullTextSearch(libraryId, null, query, 10);
    }

    @Test
    @DisplayName("should search docs with custom limit")
    void shouldSearchDocsWithCustomLimit() {
        // Arrange
        String libraryId = randomId();
        String query = "configuration";
        int limit = 5;

        when(searchService.fullTextSearch(libraryId, null, query, limit))
                .thenReturn(List.of());

        // Act
        SearchDocsResult result = searchDocsTool.searchDocs(libraryId, query, null, limit);

        // Assert
        verify(searchService).fullTextSearch(libraryId, null, query, limit);
    }

    @Test
    @DisplayName("should search docs with specific version")
    void shouldSearchDocsWithSpecificVersion() {
        // Arrange
        String libraryId = randomId();
        String query = "database";
        String version = "2.0.0";

        when(searchService.fullTextSearch(libraryId, version, query, 10))
                .thenReturn(List.of());

        // Act
        SearchDocsResult result = searchDocsTool.searchDocs(libraryId, query, version, null);

        // Assert
        verify(searchService).fullTextSearch(libraryId, version, query, 10);
    }

    @Test
    @DisplayName("should use default limit when limit is zero")
    void shouldUseDefaultLimitWhenLimitIsZero() {
        // Arrange
        String libraryId = randomId();
        String query = "test";

        when(searchService.fullTextSearch(libraryId, null, query, 10))
                .thenReturn(List.of());

        // Act
        searchDocsTool.searchDocs(libraryId, query, null, 0);

        // Assert
        verify(searchService).fullTextSearch(libraryId, null, query, 10);
    }

    @Test
    @DisplayName("should use default limit when limit is negative")
    void shouldUseDefaultLimitWhenLimitIsNegative() {
        // Arrange
        String libraryId = randomId();
        String query = "test";

        when(searchService.fullTextSearch(libraryId, null, query, 10))
                .thenReturn(List.of());

        // Act
        searchDocsTool.searchDocs(libraryId, query, null, -5);

        // Assert
        verify(searchService).fullTextSearch(libraryId, null, query, 10);
    }

    @Test
    @DisplayName("should return empty results when no matches")
    void shouldReturnEmptyResultsWhenNoMatches() {
        // Arrange
        String libraryId = randomId();
        String query = "nonexistent";

        when(searchService.fullTextSearch(libraryId, null, query, 10))
                .thenReturn(List.of());

        // Act
        SearchDocsResult result = searchDocsTool.searchDocs(libraryId, query, null, null);

        // Assert
        assertThat(result.results()).isEmpty();
        assertThat(result.total()).isEqualTo(0);
    }

    // 產生隨機 ID 的輔助方法
    private String randomId() {
        return TsidCreator.getTsid().toString();
    }
}
