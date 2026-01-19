package io.github.samzhu.docmcp;

import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingRequest;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.ai.embedding.EmbeddingResultMetadata;
import org.springframework.ai.embedding.Embedding;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;

import java.util.List;

/**
 * 測試配置類別
 * <p>
 * 提供測試所需的 Mock Bean，避免依賴外部服務。
 * </p>
 */
@TestConfiguration
@Profile("test")
public class TestConfig {

    /**
     * 提供 Mock 的 EmbeddingModel
     * <p>
     * 用於測試時替代真正的 Google GenAI Embedding。
     * 回傳固定的 768 維度向量。
     * </p>
     */
    @Bean
    public EmbeddingModel embeddingModel() {
        return new MockEmbeddingModel();
    }

    /**
     * Mock EmbeddingModel 實作
     */
    private static class MockEmbeddingModel implements EmbeddingModel {

        @Override
        public EmbeddingResponse call(EmbeddingRequest request) {
            var embeddings = request.getInstructions().stream()
                    .map(text -> new Embedding(createVector(), 0, EmbeddingResultMetadata.EMPTY))
                    .toList();
            return new EmbeddingResponse(embeddings);
        }

        @Override
        public float[] embed(String text) {
            return createVector();
        }

        @Override
        public float[] embed(Document document) {
            return createVector();
        }

        @Override
        public List<float[]> embed(List<String> texts) {
            return texts.stream()
                    .map(this::embed)
                    .toList();
        }

        @Override
        public int dimensions() {
            return 768;
        }

        private float[] createVector() {
            float[] vector = new float[768];
            for (int i = 0; i < 768; i++) {
                vector[i] = 0.1f;
            }
            return vector;
        }
    }
}
