package io.github.samzhu.docmcp.mcp.tool.retrieve;

import com.github.f4b6a3.tsid.TsidCreator;
import io.github.samzhu.docmcp.domain.exception.LibraryNotFoundException;
import io.github.samzhu.docmcp.domain.model.Document;
import io.github.samzhu.docmcp.domain.model.DocumentChunk;
import io.github.samzhu.docmcp.repository.DocumentChunkRepository;
import io.github.samzhu.docmcp.repository.DocumentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * GetRelatedDocsTool 單元測試
 */
@ExtendWith(MockitoExtension.class)
class GetRelatedDocsToolTest {

    @Mock
    private DocumentRepository documentRepository;

    @Mock
    private DocumentChunkRepository chunkRepository;

    private GetRelatedDocsTool getRelatedDocsTool;

    @BeforeEach
    void setUp() {
        getRelatedDocsTool = new GetRelatedDocsTool(documentRepository, chunkRepository);
    }

    /**
     * 產生隨機 ID
     */
    private String randomId() {
        return TsidCreator.getTsid().toString();
    }

    @Test
    @DisplayName("應回傳相關文件列表")
    void shouldReturnRelatedDocuments() {
        // Arrange
        var sourceDocId = randomId();
        var relatedDocId1 = randomId();
        var relatedDocId2 = randomId();
        var versionId = randomId();

        var sourceDoc = createDocument(sourceDocId, versionId, "Spring MVC Introduction", "docs/spring-mvc.md");
        var sourceChunk = createChunk(sourceDocId, "Introduction to Spring MVC...", new float[]{0.1f, 0.2f});

        var relatedChunk1 = createChunk(relatedDocId1, "RESTful services in Spring...", new float[]{0.15f, 0.25f});
        var relatedChunk2 = createChunk(relatedDocId2, "Web development with Spring...", new float[]{0.12f, 0.22f});

        var relatedDoc1 = createDocument(relatedDocId1, versionId, "REST Services", "docs/rest.md");
        var relatedDoc2 = createDocument(relatedDocId2, versionId, "Web Development", "docs/web.md");

        when(documentRepository.findById(sourceDocId)).thenReturn(Optional.of(sourceDoc));
        when(chunkRepository.findFirstByDocumentId(sourceDocId)).thenReturn(sourceChunk);
        when(chunkRepository.findSimilarChunksExcludingDocument(any(), anyString(), anyInt()))
                .thenReturn(List.of(relatedChunk1, relatedChunk2));
        when(documentRepository.findById(relatedDocId1)).thenReturn(Optional.of(relatedDoc1));
        when(documentRepository.findById(relatedDocId2)).thenReturn(Optional.of(relatedDoc2));

        // Act
        var result = getRelatedDocsTool.getRelatedDocs(sourceDocId, null);

        // Assert
        assertThat(result.sourceDocumentId()).isEqualTo(sourceDocId);
        assertThat(result.sourceTitle()).isEqualTo("Spring MVC Introduction");
        assertThat(result.sourcePath()).isEqualTo("docs/spring-mvc.md");
        assertThat(result.relatedDocs()).hasSize(2);
    }

    @Test
    @DisplayName("應支援限制回傳數量")
    void shouldSupportLimitParameter() {
        // Arrange
        var sourceDocId = randomId();
        var relatedDocId = randomId();
        var versionId = randomId();

        var sourceDoc = createDocument(sourceDocId, versionId, "Source Doc", "docs/source.md");
        var sourceChunk = createChunk(sourceDocId, "Source content...", new float[]{0.1f, 0.2f});

        var relatedChunk = createChunk(relatedDocId, "Related content...", new float[]{0.15f, 0.25f});
        var relatedDoc = createDocument(relatedDocId, versionId, "Related Doc", "docs/related.md");

        when(documentRepository.findById(sourceDocId)).thenReturn(Optional.of(sourceDoc));
        when(chunkRepository.findFirstByDocumentId(sourceDocId)).thenReturn(sourceChunk);
        when(chunkRepository.findSimilarChunksExcludingDocument(any(), anyString(), anyInt()))
                .thenReturn(List.of(relatedChunk));
        when(documentRepository.findById(relatedDocId)).thenReturn(Optional.of(relatedDoc));

        // Act
        var result = getRelatedDocsTool.getRelatedDocs(sourceDocId, 3);

        // Assert
        assertThat(result.relatedDocs()).hasSize(1);
    }

    @Test
    @DisplayName("當來源文件沒有 embedding 時應回傳空列表")
    void shouldReturnEmptyWhenNoEmbedding() {
        // Arrange
        var sourceDocId = randomId();
        var versionId = randomId();
        var sourceDoc = createDocument(sourceDocId, versionId, "Source Doc", "docs/source.md");

        when(documentRepository.findById(sourceDocId)).thenReturn(Optional.of(sourceDoc));
        when(chunkRepository.findFirstByDocumentId(sourceDocId)).thenReturn(null);

        // Act
        var result = getRelatedDocsTool.getRelatedDocs(sourceDocId, null);

        // Assert
        assertThat(result.relatedDocs()).isEmpty();
    }

    @Test
    @DisplayName("當找不到相關文件時應回傳空列表")
    void shouldReturnEmptyWhenNoRelatedDocs() {
        // Arrange
        var sourceDocId = randomId();
        var versionId = randomId();

        var sourceDoc = createDocument(sourceDocId, versionId, "Unique Doc", "docs/unique.md");
        var sourceChunk = createChunk(sourceDocId, "Very unique content...", new float[]{0.1f, 0.2f});

        when(documentRepository.findById(sourceDocId)).thenReturn(Optional.of(sourceDoc));
        when(chunkRepository.findFirstByDocumentId(sourceDocId)).thenReturn(sourceChunk);
        when(chunkRepository.findSimilarChunksExcludingDocument(any(), anyString(), anyInt()))
                .thenReturn(List.of());

        // Act
        var result = getRelatedDocsTool.getRelatedDocs(sourceDocId, null);

        // Assert
        assertThat(result.relatedDocs()).isEmpty();
    }

    @Test
    @DisplayName("當文件不存在時應拋出例外")
    void shouldThrowExceptionWhenDocumentNotFound() {
        // Arrange
        var docId = randomId();
        when(documentRepository.findById(docId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> getRelatedDocsTool.getRelatedDocs(docId, null))
                .isInstanceOf(LibraryNotFoundException.class)
                .hasMessageContaining("文件不存在");
    }

    /**
     * 建立測試用的 Document
     */
    private Document createDocument(String id, String versionId, String title, String path) {
        return new Document(id, versionId, title, path, "content", null, "markdown", null, null, null, null);
    }

    /**
     * 建立測試用的 DocumentChunk
     */
    private DocumentChunk createChunk(String documentId, String content, float[] embedding) {
        return new DocumentChunk(randomId(), documentId, 0, content, embedding, 100, null, null, null, null);
    }
}
