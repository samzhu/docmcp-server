package io.github.samzhu.docmcp.domain.model;

import com.github.f4b6a3.tsid.TsidCreator;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * DocumentChunk 實體測試
 */
class DocumentChunkTest {

    /**
     * 產生隨機 ID
     */
    private String randomId() {
        return TsidCreator.getTsid().toString();
    }

    @Test
    void shouldCreateChunkWithRequiredFields() {
        // 測試使用必要欄位建立區塊
        var documentId = randomId();
        var embedding = new float[768];
        for (int i = 0; i < 768; i++) {
            embedding[i] = 0.1f;
        }

        var chunk = DocumentChunk.create(
                randomId(),
                documentId,
                0,
                "This is the first chunk of content",
                embedding,
                150
        );

        assertThat(chunk.getDocumentId()).isEqualTo(documentId);
        assertThat(chunk.getChunkIndex()).isEqualTo(0);
        assertThat(chunk.getContent()).isEqualTo("This is the first chunk of content");
        assertThat(chunk.getEmbedding()).hasSize(768);
        assertThat(chunk.getTokenCount()).isEqualTo(150);
    }
}
