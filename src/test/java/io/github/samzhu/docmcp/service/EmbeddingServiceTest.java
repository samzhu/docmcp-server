package io.github.samzhu.docmcp.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.ai.embedding.EmbeddingModel;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * EmbeddingService 單元測試
 */
@DisplayName("EmbeddingService")
class EmbeddingServiceTest {

    private final EmbeddingModel mockEmbeddingModel = mock(EmbeddingModel.class);
    private final EmbeddingService embeddingService = new EmbeddingService(mockEmbeddingModel);

    @Test
    @DisplayName("should embed text successfully")
    void shouldEmbedTextSuccessfully() {
        // Arrange
        String text = "Hello, World!";
        float[] expectedVector = new float[768];
        for (int i = 0; i < 768; i++) {
            expectedVector[i] = 0.1f;
        }
        when(mockEmbeddingModel.embed(text)).thenReturn(expectedVector);

        // Act
        float[] result = embeddingService.embed(text);

        // Assert
        assertThat(result).isEqualTo(expectedVector);
        assertThat(result).hasSize(768);
    }

    @Test
    @DisplayName("should throw exception when text is null")
    void shouldThrowExceptionWhenTextIsNull() {
        // Act & Assert
        assertThatThrownBy(() -> embeddingService.embed(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("文字不得為空");
    }

    @Test
    @DisplayName("should throw exception when text is blank")
    void shouldThrowExceptionWhenTextIsBlank() {
        // Act & Assert
        assertThatThrownBy(() -> embeddingService.embed("   "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("文字不得為空");
    }

    @Test
    @DisplayName("should embed batch of texts successfully")
    void shouldEmbedBatchSuccessfully() {
        // Arrange
        List<String> texts = List.of("Hello", "World");
        float[] vector1 = new float[768];
        float[] vector2 = new float[768];
        for (int i = 0; i < 768; i++) {
            vector1[i] = 0.1f;
            vector2[i] = 0.2f;
        }
        when(mockEmbeddingModel.embed(texts)).thenReturn(List.of(vector1, vector2));

        // Act
        List<float[]> results = embeddingService.embedBatch(texts);

        // Assert
        assertThat(results).hasSize(2);
        assertThat(results.get(0)).isEqualTo(vector1);
        assertThat(results.get(1)).isEqualTo(vector2);
    }

    @Test
    @DisplayName("should throw exception when batch is null")
    void shouldThrowExceptionWhenBatchIsNull() {
        // Act & Assert
        assertThatThrownBy(() -> embeddingService.embedBatch(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("文字列表不得為空");
    }

    @Test
    @DisplayName("should throw exception when batch is empty")
    void shouldThrowExceptionWhenBatchIsEmpty() {
        // Act & Assert
        assertThatThrownBy(() -> embeddingService.embedBatch(List.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("文字列表不得為空");
    }

    @Test
    @DisplayName("should return correct dimensions")
    void shouldReturnCorrectDimensions() {
        // Arrange
        when(mockEmbeddingModel.dimensions()).thenReturn(768);

        // Act
        int dimensions = embeddingService.getDimensions();

        // Assert
        assertThat(dimensions).isEqualTo(768);
    }

    @Test
    @DisplayName("should convert embedding to vector string")
    void shouldConvertEmbeddingToVectorString() {
        // Arrange
        float[] embedding = new float[]{0.1f, 0.2f, 0.3f};

        // Act
        String result = embeddingService.toVectorString(embedding);

        // Assert
        assertThat(result).isEqualTo("[0.1,0.2,0.3]");
    }

    @Test
    @DisplayName("should throw exception when embedding is null for toVectorString")
    void shouldThrowExceptionWhenEmbeddingIsNullForVectorString() {
        // Act & Assert
        assertThatThrownBy(() -> embeddingService.toVectorString(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("向量不得為空");
    }

    @Test
    @DisplayName("should throw exception when embedding is empty for toVectorString")
    void shouldThrowExceptionWhenEmbeddingIsEmptyForVectorString() {
        // Act & Assert
        assertThatThrownBy(() -> embeddingService.toVectorString(new float[0]))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("向量不得為空");
    }
}
