package io.github.samzhu.docmcp.domain.model;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Document 實體測試
 */
class DocumentTest {

    @Test
    void shouldCreateDocumentWithRequiredFields() {
        // 測試使用必要欄位建立文件
        var versionId = UUID.randomUUID();
        var document = Document.create(
                versionId,
                "Getting Started",
                "/docs/getting-started.md",
                "# Getting Started\n\nThis is the content...",
                "abc123",
                "markdown"
        );

        assertThat(document.versionId()).isEqualTo(versionId);
        assertThat(document.title()).isEqualTo("Getting Started");
        assertThat(document.path()).isEqualTo("/docs/getting-started.md");
        assertThat(document.content()).startsWith("# Getting Started");
        assertThat(document.docType()).isEqualTo("markdown");
    }
}
