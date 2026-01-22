package io.github.samzhu.docmcp.mcp.tool.retrieve;

import com.github.f4b6a3.tsid.TsidCreator;
import io.github.samzhu.docmcp.domain.exception.DocumentNotFoundException;
import io.github.samzhu.docmcp.domain.model.CodeExample;
import io.github.samzhu.docmcp.domain.model.Document;
import io.github.samzhu.docmcp.domain.model.DocumentChunk;
import io.github.samzhu.docmcp.mcp.dto.GetDocContentResult;
import io.github.samzhu.docmcp.service.DocumentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * GetDocContentTool 單元測試
 */
@DisplayName("GetDocContentTool")
class GetDocContentToolTest {

    private DocumentService documentService;
    private GetDocContentTool getDocContentTool;

    @BeforeEach
    void setUp() {
        documentService = mock(DocumentService.class);
        getDocContentTool = new GetDocContentTool(documentService);
    }

    /**
     * 產生隨機 ID
     */
    private String randomId() {
        return TsidCreator.getTsid().toString();
    }

    @Test
    @DisplayName("should return document content with code examples by default")
    void shouldReturnDocumentContentWithCodeExamplesByDefault() {
        // Arrange
        String documentId = randomId();
        var document = createDocument(documentId, "Test Doc", "/docs/test.md", "Content");
        var chunk = createChunk(randomId(), documentId, 0, "Chunk content");
        var example = createCodeExample(randomId(), documentId, "java", "System.out.println();");

        var content = new DocumentService.DocumentContent(document, List.of(chunk), List.of(example));
        when(documentService.getDocumentContent(documentId)).thenReturn(content);

        // Act
        GetDocContentResult result = getDocContentTool.getDocContent(documentId, null);

        // Assert
        assertThat(result.document().id()).isEqualTo(documentId);
        assertThat(result.document().title()).isEqualTo("Test Doc");
        assertThat(result.document().chunks()).hasSize(1);
        assertThat(result.codeExamples()).hasSize(1);
    }

    @Test
    @DisplayName("should return document content with code examples when includeCodeExamples is true")
    void shouldReturnDocumentContentWithCodeExamplesWhenTrue() {
        // Arrange
        String documentId = randomId();
        var document = createDocument(documentId, "Test Doc", "/docs/test.md", "Content");
        var example = createCodeExample(randomId(), documentId, "java", "code");

        var content = new DocumentService.DocumentContent(document, List.of(), List.of(example));
        when(documentService.getDocumentContent(documentId)).thenReturn(content);

        // Act
        GetDocContentResult result = getDocContentTool.getDocContent(documentId, true);

        // Assert
        assertThat(result.codeExamples()).hasSize(1);
    }

    @Test
    @DisplayName("should return document content without code examples when includeCodeExamples is false")
    void shouldReturnDocumentContentWithoutCodeExamplesWhenFalse() {
        // Arrange
        String documentId = randomId();
        var document = createDocument(documentId, "Test Doc", "/docs/test.md", "Content");
        var example = createCodeExample(randomId(), documentId, "java", "code");

        var content = new DocumentService.DocumentContent(document, List.of(), List.of(example));
        when(documentService.getDocumentContent(documentId)).thenReturn(content);

        // Act
        GetDocContentResult result = getDocContentTool.getDocContent(documentId, false);

        // Assert
        assertThat(result.codeExamples()).isEmpty();
    }

    @Test
    @DisplayName("should throw exception when document not found")
    void shouldThrowExceptionWhenDocumentNotFound() {
        // Arrange
        String documentId = randomId();
        when(documentService.getDocumentContent(documentId))
                .thenThrow(DocumentNotFoundException.byId(documentId));

        // Act & Assert
        assertThatThrownBy(() -> getDocContentTool.getDocContent(documentId, null))
                .isInstanceOf(DocumentNotFoundException.class);
    }

    // Helper methods
    /**
     * 建立測試用的 Document
     */
    private Document createDocument(String id, String title, String path, String content) {
        return new Document(id, randomId(), title, path, content, "hash",
                "markdown", Map.of(), null, OffsetDateTime.now(), OffsetDateTime.now());
    }

    /**
     * 建立測試用的 DocumentChunk
     */
    private DocumentChunk createChunk(String id, String documentId, int index, String content) {
        var now = OffsetDateTime.now();
        return new DocumentChunk(id, documentId, index, content, null, 100, Map.of(), null, now, now);
    }

    /**
     * 建立測試用的 CodeExample
     */
    private CodeExample createCodeExample(String id, String documentId, String language, String code) {
        var now = OffsetDateTime.now();
        return new CodeExample(id, documentId, language, code, "Description",
                null, null, Map.of(), null, now, now);
    }
}
