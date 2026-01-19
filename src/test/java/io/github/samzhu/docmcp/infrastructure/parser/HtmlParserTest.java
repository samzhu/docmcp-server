package io.github.samzhu.docmcp.infrastructure.parser;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * HtmlParser 單元測試
 */
@DisplayName("HtmlParser")
class HtmlParserTest {

    private HtmlParser parser;

    @BeforeEach
    void setUp() {
        parser = new HtmlParser();
    }

    @Test
    @DisplayName("should parse HTML with title tag")
    void shouldParseHtmlWithTitleTag() {
        // Arrange
        String content = """
                <!DOCTYPE html>
                <html>
                <head>
                    <title>Getting Started Guide</title>
                </head>
                <body>
                    <p>Welcome to the guide.</p>
                </body>
                </html>
                """;

        // Act
        ParsedDocument result = parser.parse(content, "guide.html");

        // Assert
        assertThat(result.title()).isEqualTo("Getting Started Guide");
    }

    @Test
    @DisplayName("should extract title from H1 when no title tag")
    void shouldExtractTitleFromH1WhenNoTitleTag() {
        // Arrange
        String content = """
                <html>
                <body>
                    <h1>My Document</h1>
                    <p>Content here.</p>
                </body>
                </html>
                """;

        // Act
        ParsedDocument result = parser.parse(content, "doc.html");

        // Assert
        assertThat(result.title()).isEqualTo("My Document");
    }

    @Test
    @DisplayName("should extract code blocks")
    void shouldExtractCodeBlocks() {
        // Arrange
        String content = """
                <html>
                <body>
                    <pre><code class="language-java">
                    public class Test {}
                    </code></pre>
                </body>
                </html>
                """;

        // Act
        ParsedDocument result = parser.parse(content, "example.html");

        // Assert
        assertThat(result.codeBlocks()).hasSize(1);
        assertThat(result.codeBlocks().get(0).language()).isEqualTo("java");
    }

    @Test
    @DisplayName("should detect language from class attribute")
    void shouldDetectLanguageFromClassAttribute() {
        // Arrange
        String content = """
                <html>
                <body>
                    <pre><code class="lang-python">print("hello")</code></pre>
                </body>
                </html>
                """;

        // Act
        ParsedDocument result = parser.parse(content, "example.html");

        // Assert
        assertThat(result.codeBlocks().get(0).language()).isEqualTo("python");
    }

    @Test
    @DisplayName("should convert HTML to Markdown")
    void shouldConvertHtmlToMarkdown() {
        // Arrange
        String content = """
                <html>
                <body>
                    <h2>Section</h2>
                    <p>This is a paragraph.</p>
                    <ul>
                        <li>Item 1</li>
                        <li>Item 2</li>
                    </ul>
                </body>
                </html>
                """;

        // Act
        ParsedDocument result = parser.parse(content, "doc.html");

        // Assert - content should contain the text (markdown format may vary)
        assertThat(result.content()).contains("Section");
        assertThat(result.content()).contains("This is a paragraph");
    }

    @Test
    @DisplayName("should handle empty content")
    void shouldHandleEmptyContent() {
        // Act
        ParsedDocument result = parser.parse("", "empty.html");

        // Assert
        assertThat(result.title()).isEmpty();
        assertThat(result.content()).isEmpty();
    }

    @Test
    @DisplayName("should support .html extension")
    void shouldSupportHtmlExtension() {
        assertThat(parser.supports("docs/page.html")).isTrue();
        assertThat(parser.supports("DOCS/PAGE.HTML")).isTrue();
    }

    @Test
    @DisplayName("should support .htm extension")
    void shouldSupportHtmExtension() {
        assertThat(parser.supports("docs/page.htm")).isTrue();
    }

    @Test
    @DisplayName("should not support other extensions")
    void shouldNotSupportOtherExtensions() {
        assertThat(parser.supports("docs/readme.md")).isFalse();
        assertThat(parser.supports("docs/readme.adoc")).isFalse();
    }

    @Test
    @DisplayName("should return correct doc type")
    void shouldReturnCorrectDocType() {
        assertThat(parser.getDocType()).isEqualTo("html");
    }
}
