package io.github.samzhu.docmcp.mcp.tool.retrieve;

import io.github.samzhu.docmcp.domain.exception.LibraryNotFoundException;
import io.github.samzhu.docmcp.domain.model.DocumentChunk;
import io.github.samzhu.docmcp.mcp.dto.GetRelatedDocsResult;
import io.github.samzhu.docmcp.repository.DocumentChunkRepository;
import io.github.samzhu.docmcp.repository.DocumentRepository;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * 取得相關文件工具
 * <p>
 * MCP Level 3 - Retrieve 工具，用於查找與指定文件相似的其他文件。
 * 基於向量嵌入的內容相似度進行搜尋。
 * </p>
 */
@Component
public class GetRelatedDocsTool {

    private static final int DEFAULT_LIMIT = 5;

    private final DocumentRepository documentRepository;
    private final DocumentChunkRepository chunkRepository;

    public GetRelatedDocsTool(DocumentRepository documentRepository,
                              DocumentChunkRepository chunkRepository) {
        this.documentRepository = documentRepository;
        this.chunkRepository = chunkRepository;
    }

    /**
     * 查找與指定文件相關的其他文件
     * <p>
     * 此工具使用向量相似度搜尋，找出與指定文件內容相似的其他文件。
     * AI 助手可用此工具協助使用者探索相關主題。
     * </p>
     *
     * @param documentId 文件 ID
     * @param limit      最大回傳數量（可選，預設 5）
     * @return 相關文件列表
     */
    @Tool(name = "get_related_docs",
            description = """
                    查找與指定文件相關的其他文件。

                    使用時機：
                    - 當使用者閱讀某篇文件後想看相關內容時
                    - 當需要探索特定主題的更多資料時
                    - 當需要建立文件間的關聯時

                    回傳：相關文件列表，包含標題、路徑和相似度分數。
                    """)
    public GetRelatedDocsResult getRelatedDocs(
            @ToolParam(description = "文件 ID", required = true)
            String documentId,
            @ToolParam(description = "最大回傳數量（可選，預設 5）", required = false)
            Integer limit
    ) {
        var docId = UUID.fromString(documentId);

        // 取得來源文件
        var sourceDoc = documentRepository.findById(docId)
                .orElseThrow(() -> new LibraryNotFoundException("文件不存在: " + documentId));

        // 取得來源文件的第一個區塊（代表向量）
        var sourceChunk = chunkRepository.findFirstByDocumentId(docId);
        if (sourceChunk == null || sourceChunk.embedding() == null) {
            // 沒有 embedding，回傳空結果
            return new GetRelatedDocsResult(
                    documentId,
                    sourceDoc.title(),
                    sourceDoc.path(),
                    List.of()
            );
        }

        // 設定限制數量
        int maxResults = limit != null && limit > 0 ? limit : DEFAULT_LIMIT;

        // 搜尋相似區塊（排除來源文件）
        String vectorString = toVectorString(sourceChunk.embedding());
        var similarChunks = chunkRepository.findSimilarChunksExcludingDocument(
                docId, vectorString, maxResults * 2);  // 多取一些以確保去重後有足夠數量

        // 去重並轉換為文件
        List<GetRelatedDocsResult.RelatedDoc> relatedDocs = new ArrayList<>();
        Set<UUID> seenDocIds = new HashSet<>();

        for (DocumentChunk chunk : similarChunks) {
            if (seenDocIds.contains(chunk.documentId())) {
                continue;  // 跳過已加入的文件
            }
            if (relatedDocs.size() >= maxResults) {
                break;  // 已達到限制數量
            }

            var relatedDoc = documentRepository.findById(chunk.documentId()).orElse(null);
            if (relatedDoc != null) {
                seenDocIds.add(chunk.documentId());

                // 計算相似度
                double similarity = calculateCosineSimilarity(
                        sourceChunk.embedding(), chunk.embedding());

                relatedDocs.add(new GetRelatedDocsResult.RelatedDoc(
                        relatedDoc.id().toString(),
                        relatedDoc.title(),
                        relatedDoc.path(),
                        similarity,
                        truncateContent(chunk.content(), 200)
                ));
            }
        }

        return new GetRelatedDocsResult(
                documentId,
                sourceDoc.title(),
                sourceDoc.path(),
                relatedDocs
        );
    }

    /**
     * 計算餘弦相似度
     */
    private double calculateCosineSimilarity(float[] a, float[] b) {
        if (a == null || b == null || a.length != b.length) {
            return 0.0;
        }

        double dotProduct = 0.0;
        double normA = 0.0;
        double normB = 0.0;

        for (int i = 0; i < a.length; i++) {
            dotProduct += a[i] * b[i];
            normA += a[i] * a[i];
            normB += b[i] * b[i];
        }

        if (normA == 0 || normB == 0) {
            return 0.0;
        }

        return dotProduct / (Math.sqrt(normA) * Math.sqrt(normB));
    }

    /**
     * 截斷內容
     */
    private String truncateContent(String content, int maxLength) {
        if (content == null) {
            return "";
        }
        if (content.length() <= maxLength) {
            return content;
        }
        return content.substring(0, maxLength) + "...";
    }

    /**
     * 將 float[] 轉換為 PostgreSQL vector 字串格式
     *
     * @param embedding 向量陣列
     * @return PostgreSQL vector 格式字串，如 "[0.1, 0.2, ...]"
     */
    private String toVectorString(float[] embedding) {
        if (embedding == null || embedding.length == 0) {
            throw new IllegalArgumentException("向量不得為空");
        }
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < embedding.length; i++) {
            if (i > 0) {
                sb.append(",");
            }
            sb.append(embedding[i]);
        }
        sb.append("]");
        return sb.toString();
    }
}
