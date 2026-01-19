package io.github.samzhu.docmcp.infrastructure.parser;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * MarkdownParser 單元測試
 */
@DisplayName("MarkdownParser")
class MarkdownParserTest {

    private MarkdownParser parser;

    @BeforeEach
    void setUp() {
        parser = new MarkdownParser();
    }

    @Test
    @DisplayName("should parse markdown with title")
    void shouldParseMarkdownWithTitle() {
        // Arrange
        String content = """
                # Getting Started

                This is a guide to get started.
                """;

        // Act
        ParsedDocument result = parser.parse(content, "getting-started.md");

        // Assert
        assertThat(result.title()).isEqualTo("Getting Started");
        assertThat(result.content()).isEqualTo(content);
    }

    @Test
    @DisplayName("should extract code blocks")
    void shouldExtractCodeBlocks() {
        // Arrange
        String content = """
                # Example

                Here is a Java example:

                ```java
                public class Hello {
                    public static void main(String[] args) {
                        System.out.println("Hello");
                    }
                }
                ```

                And a JavaScript example:

                ```javascript
                console.log("Hello");
                ```
                """;

        // Act
        ParsedDocument result = parser.parse(content, "example.md");

        // Assert
        assertThat(result.codeBlocks()).hasSize(2);
        assertThat(result.codeBlocks().get(0).language()).isEqualTo("java");
        assertThat(result.codeBlocks().get(0).code()).contains("public class Hello");
        assertThat(result.codeBlocks().get(1).language()).isEqualTo("javascript");
    }

    @Test
    @DisplayName("should use filename as title when no H1 found")
    void shouldUseFilenameAsTitleWhenNoH1Found() {
        // Arrange
        String content = """
                ## Section 1

                Some content here.
                """;

        // Act
        ParsedDocument result = parser.parse(content, "docs/my-document.md");

        // Assert
        assertThat(result.title()).isEqualTo("my-document");
    }

    @Test
    @DisplayName("should handle empty content")
    void shouldHandleEmptyContent() {
        // Act
        ParsedDocument result = parser.parse("", "empty.md");

        // Assert
        assertThat(result.title()).isEmpty();
        assertThat(result.content()).isEmpty();
        assertThat(result.codeBlocks()).isEmpty();
    }

    @Test
    @DisplayName("should handle null content")
    void shouldHandleNullContent() {
        // Act
        ParsedDocument result = parser.parse(null, "null.md");

        // Assert
        assertThat(result.title()).isEmpty();
        assertThat(result.content()).isEmpty();
        assertThat(result.codeBlocks()).isEmpty();
    }

    @Test
    @DisplayName("should support .md extension")
    void shouldSupportMdExtension() {
        assertThat(parser.supports("docs/readme.md")).isTrue();
        assertThat(parser.supports("DOCS/README.MD")).isTrue();
    }

    @Test
    @DisplayName("should support .markdown extension")
    void shouldSupportMarkdownExtension() {
        assertThat(parser.supports("docs/readme.markdown")).isTrue();
    }

    @Test
    @DisplayName("should not support other extensions")
    void shouldNotSupportOtherExtensions() {
        assertThat(parser.supports("docs/readme.html")).isFalse();
        assertThat(parser.supports("docs/readme.adoc")).isFalse();
        assertThat(parser.supports("docs/readme.txt")).isFalse();
    }

    @Test
    @DisplayName("should return correct doc type")
    void shouldReturnCorrectDocType() {
        assertThat(parser.getDocType()).isEqualTo("markdown");
    }

    @Test
    @DisplayName("should set metadata correctly")
    void shouldSetMetadataCorrectly() {
        // Arrange
        String content = """
                # Test

                ```python
                print("Hello")
                ```
                """;

        // Act
        ParsedDocument result = parser.parse(content, "test.md");

        // Assert
        assertThat(result.metadata()).containsEntry("path", "test.md");
        assertThat(result.metadata()).containsEntry("format", "markdown");
        assertThat(result.metadata()).containsEntry("codeBlockCount", 1);
    }
}
