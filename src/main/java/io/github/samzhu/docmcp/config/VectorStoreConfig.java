package io.github.samzhu.docmcp.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.samzhu.docmcp.infrastructure.vectorstore.DocumentChunkVectorStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.pgvector.autoconfigure.PgVectorStoreProperties;
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
     *   <li>透過 JSONPath 進行 metadata 過濾</li>
     *   <li>使用 JdbcTemplate + PGvector 物件（參考 Spring AI 官方實作）</li>
     *   <li>與 Spring AI RAG Advisor 等功能相容</li>
     * </ul>
     * </p>
     *
     * @param jdbcTemplate            JDBC 操作模板
     * @param embeddingModel          嵌入模型（Google GenAI 或 Mock）
     * @param objectMapper            JSON 序列化工具
     * @param pgVectorStoreProperties PgVector 配置屬性（從 spring.ai.vectorstore.pgvector.* 讀取）
     * @return VectorStore 實例
     */
    @Bean
    @Primary
    public VectorStore documentChunkVectorStore(
            JdbcTemplate jdbcTemplate,
            EmbeddingModel embeddingModel,
            ObjectMapper objectMapper,
            PgVectorStoreProperties pgVectorStoreProperties) {

        int dimensions = pgVectorStoreProperties.getDimensions();
        log.info("初始化 DocumentChunkVectorStore，向量維度: {}", dimensions);

        return new DocumentChunkVectorStore(jdbcTemplate, embeddingModel, objectMapper, dimensions);
    }
}
