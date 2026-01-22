package io.github.samzhu.docmcp.domain.model;

import com.github.f4b6a3.tsid.TsidCreator;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Document 實體測試
 */
class DocumentTest {

    /**
     * 產生隨機 ID
     */
    private String randomId() {
        return TsidCreator.getTsid().toString();
    }

    @Test
    void shouldCreateDocumentWithRequiredFields() {
        // 測試使用必要欄位建立文件
        var versionId = randomId();
        var document = Document.create(
                randomId(),
                versionId,
                "Getting Started",
                "/docs/getting-started.md",
                "# Getting Started\n\nThis is the content...",
                "abc123",
                "markdown"
        );

        assertThat(document.getVersionId()).isEqualTo(versionId);
        assertThat(document.getTitle()).isEqualTo("Getting Started");
        assertThat(document.getPath()).isEqualTo("/docs/getting-started.md");
        assertThat(document.getContent()).startsWith("# Getting Started");
        assertThat(document.getDocType()).isEqualTo("markdown");
    }
}
