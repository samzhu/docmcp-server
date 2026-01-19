package io.github.samzhu.docmcp.domain.model;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * DocumentChunk 實體測試
 */
class DocumentChunkTest {

    @Test
    void shouldCreateChunkWithRequiredFields() {
        // 測試使用必要欄位建立區塊
        var documentId = UUID.randomUUID();
        var embedding = new float[768];
        for (int i = 0; i < 768; i++) {
            embedding[i] = 0.1f;
        }

        var chunk = DocumentChunk.create(
                documentId,
                0,
                "This is the first chunk of content",
                embedding,
                150
        );

        assertThat(chunk.documentId()).isEqualTo(documentId);
        assertThat(chunk.chunkIndex()).isEqualTo(0);
        assertThat(chunk.content()).isEqualTo("This is the first chunk of content");
        assertThat(chunk.embedding()).hasSize(768);
        assertThat(chunk.tokenCount()).isEqualTo(150);
    }
}
