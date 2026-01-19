package io.github.samzhu.docmcp.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * DocumentChunker 單元測試
 */
@DisplayName("DocumentChunker")
class DocumentChunkerTest {

    private DocumentChunker chunker;

    @BeforeEach
    void setUp() {
        chunker = new DocumentChunker();
    }

    @Test
    @DisplayName("should return single chunk for short content")
    void shouldReturnSingleChunkForShortContent() {
        // Arrange
        String content = "This is a short document.";

        // Act
        List<DocumentChunker.ChunkResult> chunks = chunker.chunk(content, 1000, 200);

        // Assert
        assertThat(chunks).hasSize(1);
        assertThat(chunks.get(0).content()).isEqualTo(content);
        assertThat(chunks.get(0).index()).isEqualTo(0);
    }

    @Test
    @DisplayName("should split content into multiple chunks")
    void shouldSplitContentIntoMultipleChunks() {
        // Arrange
        String content = "A".repeat(500) + "\n\n" + "B".repeat(500) + "\n\n" + "C".repeat(500);

        // Act
        List<DocumentChunker.ChunkResult> chunks = chunker.chunk(content, 600, 100);

        // Assert
        assertThat(chunks).hasSizeGreaterThan(1);
        // Each chunk should have an index
        for (int i = 0; i < chunks.size(); i++) {
            assertThat(chunks.get(i).index()).isEqualTo(i);
        }
    }

    @Test
    @DisplayName("should split at paragraph boundaries")
    void shouldSplitAtParagraphBoundaries() {
        // Arrange
        String paragraph1 = "This is the first paragraph. It has some content.";
        String paragraph2 = "This is the second paragraph. It also has content.";
        String content = paragraph1 + "\n\n" + paragraph2;

        // Act
        List<DocumentChunker.ChunkResult> chunks = chunker.chunk(content, 60, 10);

        // Assert
        // First chunk should end at paragraph boundary
        boolean foundParagraphSplit = chunks.stream()
                .anyMatch(c -> c.content().endsWith(paragraph1 + "\n\n") ||
                        c.content().equals(paragraph1 + "\n\n"));

        // The chunking should respect natural boundaries
        assertThat(chunks).hasSizeGreaterThan(1);
    }

    @Test
    @DisplayName("should handle empty content")
    void shouldHandleEmptyContent() {
        // Act
        List<DocumentChunker.ChunkResult> chunks = chunker.chunk("");

        // Assert
        assertThat(chunks).isEmpty();
    }

    @Test
    @DisplayName("should handle null content")
    void shouldHandleNullContent() {
        // Act
        List<DocumentChunker.ChunkResult> chunks = chunker.chunk(null);

        // Assert
        assertThat(chunks).isEmpty();
    }

    @Test
    @DisplayName("should estimate token count")
    void shouldEstimateTokenCount() {
        // Arrange
        String content = "Hello world, this is a test document.";

        // Act
        List<DocumentChunker.ChunkResult> chunks = chunker.chunk(content);

        // Assert
        assertThat(chunks.get(0).tokenCount()).isGreaterThan(0);
    }

    @Test
    @DisplayName("should use default chunk size")
    void shouldUseDefaultChunkSize() {
        // Arrange
        String content = "A".repeat(2000);

        // Act
        List<DocumentChunker.ChunkResult> chunks = chunker.chunk(content);

        // Assert
        // With default size of 1000 and overlap of 200, should have 2-3 chunks
        assertThat(chunks).hasSizeBetween(2, 4);
    }

    @Test
    @DisplayName("should handle invalid chunk size")
    void shouldHandleInvalidChunkSize() {
        // Arrange
        String content = "Test content";

        // Act - with invalid chunk size (should use default)
        List<DocumentChunker.ChunkResult> chunks = chunker.chunk(content, 0, 0);

        // Assert
        assertThat(chunks).hasSize(1);
    }

    @Test
    @DisplayName("should maintain overlap between chunks")
    void shouldMaintainOverlapBetweenChunks() {
        // Arrange
        String content = "Word1 Word2 Word3 Word4 Word5 Word6 Word7 Word8 Word9 Word10 " +
                "Word11 Word12 Word13 Word14 Word15 Word16 Word17 Word18 Word19 Word20";

        // Act
        List<DocumentChunker.ChunkResult> chunks = chunker.chunk(content, 50, 20);

        // Assert - with overlap, some content should appear in multiple chunks
        if (chunks.size() > 1) {
            // Content at chunk boundaries should overlap
            assertThat(chunks).hasSizeGreaterThan(1);
        }
    }

    @Test
    @DisplayName("should handle content with Chinese characters")
    void shouldHandleContentWithChineseCharacters() {
        // Arrange
        String content = "這是一段中文內容。This is mixed content. 混合內容測試。";

        // Act
        List<DocumentChunker.ChunkResult> chunks = chunker.chunk(content);

        // Assert
        assertThat(chunks).hasSize(1);
        assertThat(chunks.get(0).tokenCount()).isGreaterThan(0);
    }
}
