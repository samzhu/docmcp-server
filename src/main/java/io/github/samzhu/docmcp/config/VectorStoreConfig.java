package io.github.samzhu.docmcp.config;

import io.github.samzhu.docmcp.infrastructure.vectorstore.DocumentChunkVectorStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * VectorStore 配置類別
 * <p>
 * 配置自訂的 DocumentChunkVectorStore 作為系統的 VectorStore 實作。
 * 這個實作使用現有的 document_chunks 表結構，並與 Spring AI 生態系統相容。
 * </p>
 * <p>
 * 使用 @Primary 註解確保在有多個 VectorStore 實作時，
 * 系統預設使用 DocumentChunkVectorStore。
 * </p>
 */
@Configuration
public class VectorStoreConfig {

    private static final Logger log = LoggerFactory.getLogger(VectorStoreConfig.class);

    /**
     * 建立 DocumentChunkVectorStore Bean
     * <p>
     * 這是系統主要的 VectorStore 實作，支援：
     * <ul>
     *   <li>批次 embedding 生成（使用 EmbeddingModel）</li>
     *   <li>向量相似度搜尋（使用 pgvector）</li>
     *   <li>透過 metadata 進行過濾（支援 versionId 等）</li>
     *   <li>與 Spring AI RAG Advisor 等功能相容</li>
     * </ul>
     * </p>
     *
     * @param jdbcTemplate   JDBC 操作模板
     * @param embeddingModel 嵌入模型（Google GenAI 或 Mock）
     * @param dimensions     向量維度（從配置讀取，預設 768）
     * @return VectorStore 實例
     */
    @Bean
    @Primary
    public VectorStore documentChunkVectorStore(
            JdbcTemplate jdbcTemplate,
            EmbeddingModel embeddingModel,
            @Value("${spring.ai.vectorstore.pgvector.dimensions:768}") int dimensions) {

        log.info("初始化 DocumentChunkVectorStore，向量維度: {}", dimensions);

        return new DocumentChunkVectorStore(jdbcTemplate, embeddingModel, dimensions);
    }
}
