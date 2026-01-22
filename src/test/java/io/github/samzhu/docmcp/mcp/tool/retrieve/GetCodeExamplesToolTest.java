package io.github.samzhu.docmcp.mcp.tool.retrieve;

import com.github.f4b6a3.tsid.TsidCreator;
import io.github.samzhu.docmcp.domain.model.CodeExample;
import io.github.samzhu.docmcp.mcp.dto.GetCodeExamplesResult;
import io.github.samzhu.docmcp.service.DocumentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * GetCodeExamplesTool 單元測試
 */
@DisplayName("GetCodeExamplesTool")
class GetCodeExamplesToolTest {

    private DocumentService documentService;
    private GetCodeExamplesTool getCodeExamplesTool;

    @BeforeEach
    void setUp() {
        documentService = mock(DocumentService.class);
        getCodeExamplesTool = new GetCodeExamplesTool(documentService);
    }

    /**
     * 產生隨機 ID
     */
    private String randomId() {
        return TsidCreator.getTsid().toString();
    }

    @Test
    @DisplayName("should return code examples with default parameters")
    void shouldReturnCodeExamplesWithDefaultParameters() {
        // Arrange
        String libraryId = randomId();
        var example = createCodeExample(randomId(), randomId(), "java",
                "System.out.println(\"Hello\");", "Hello World example");

        when(documentService.getCodeExamples(libraryId, null, null, 10))
                .thenReturn(List.of(example));

        // Act
        GetCodeExamplesResult result = getCodeExamplesTool.getCodeExamples(libraryId, null, null, null);

        // Assert
        assertThat(result.examples()).hasSize(1);
        assertThat(result.total()).isEqualTo(1);
        assertThat(result.language()).isNull();
        verify(documentService).getCodeExamples(libraryId, null, null, 10);
    }

    @Test
    @DisplayName("should filter by language")
    void shouldFilterByLanguage() {
        // Arrange
        String libraryId = randomId();
        String language = "javascript";
        var example = createCodeExample(randomId(), randomId(), language,
                "console.log('Hello');", "JS example");

        when(documentService.getCodeExamples(libraryId, null, language, 10))
                .thenReturn(List.of(example));

        // Act
        GetCodeExamplesResult result = getCodeExamplesTool.getCodeExamples(libraryId, null, language, null);

        // Assert
        assertThat(result.examples()).hasSize(1);
        assertThat(result.examples().getFirst().language()).isEqualTo(language);
        assertThat(result.language()).isEqualTo(language);
    }

    @Test
    @DisplayName("should use specified version")
    void shouldUseSpecifiedVersion() {
        // Arrange
        String libraryId = randomId();
        String version = "2.0.0";

        when(documentService.getCodeExamples(libraryId, version, null, 10))
                .thenReturn(List.of());

        // Act
        getCodeExamplesTool.getCodeExamples(libraryId, version, null, null);

        // Assert
        verify(documentService).getCodeExamples(libraryId, version, null, 10);
    }

    @Test
    @DisplayName("should use custom limit")
    void shouldUseCustomLimit() {
        // Arrange
        String libraryId = randomId();
        int limit = 5;

        when(documentService.getCodeExamples(libraryId, null, null, limit))
                .thenReturn(List.of());

        // Act
        getCodeExamplesTool.getCodeExamples(libraryId, null, null, limit);

        // Assert
        verify(documentService).getCodeExamples(libraryId, null, null, limit);
    }

    @Test
    @DisplayName("should use default limit when limit is zero")
    void shouldUseDefaultLimitWhenLimitIsZero() {
        // Arrange
        String libraryId = randomId();

        when(documentService.getCodeExamples(libraryId, null, null, 10))
                .thenReturn(List.of());

        // Act
        getCodeExamplesTool.getCodeExamples(libraryId, null, null, 0);

        // Assert
        verify(documentService).getCodeExamples(libraryId, null, null, 10);
    }

    @Test
    @DisplayName("should use default limit when limit is negative")
    void shouldUseDefaultLimitWhenLimitIsNegative() {
        // Arrange
        String libraryId = randomId();

        when(documentService.getCodeExamples(libraryId, null, null, 10))
                .thenReturn(List.of());

        // Act
        getCodeExamplesTool.getCodeExamples(libraryId, null, null, -5);

        // Assert
        verify(documentService).getCodeExamples(libraryId, null, null, 10);
    }

    @Test
    @DisplayName("should return empty results when no examples found")
    void shouldReturnEmptyResultsWhenNoExamplesFound() {
        // Arrange
        String libraryId = randomId();

        when(documentService.getCodeExamples(libraryId, null, null, 10))
                .thenReturn(List.of());

        // Act
        GetCodeExamplesResult result = getCodeExamplesTool.getCodeExamples(libraryId, null, null, null);

        // Assert
        assertThat(result.examples()).isEmpty();
        assertThat(result.total()).isEqualTo(0);
    }

    @Test
    @DisplayName("should return multiple examples")
    void shouldReturnMultipleExamples() {
        // Arrange
        String libraryId = randomId();
        var example1 = createCodeExample(randomId(), randomId(), "java", "code1", "Desc 1");
        var example2 = createCodeExample(randomId(), randomId(), "java", "code2", "Desc 2");

        when(documentService.getCodeExamples(libraryId, null, null, 10))
                .thenReturn(List.of(example1, example2));

        // Act
        GetCodeExamplesResult result = getCodeExamplesTool.getCodeExamples(libraryId, null, null, null);

        // Assert
        assertThat(result.examples()).hasSize(2);
        assertThat(result.total()).isEqualTo(2);
    }

    // Helper method
    /**
     * 建立測試用的 CodeExample
     */
    private CodeExample createCodeExample(String id, String documentId, String language,
                                           String code, String description) {
        var now = OffsetDateTime.now();
        return new CodeExample(id, documentId, language, code, description,
                null, null, Map.of(), null, now, now);
    }
}
