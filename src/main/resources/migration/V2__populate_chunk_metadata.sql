-- =============================================================================
-- 資料遷移腳本：填入 document_chunks.metadata 欄位
-- =============================================================================
-- 此腳本將 versionId、documentId 等資訊反正規化到 document_chunks.metadata 欄位，
-- 以支援 Spring AI VectorStore 的 filter 機制，無需 JOIN 即可進行版本過濾。
-- =============================================================================

-- 1. 更新現有的 document_chunks，將必要資訊填入 metadata JSONB 欄位
UPDATE document_chunks dc
SET metadata = COALESCE(dc.metadata, '{}'::jsonb) || jsonb_build_object(
    'versionId', d.version_id::text,
    'documentId', dc.document_id::text,
    'chunkIndex', dc.chunk_index,
    'tokenCount', COALESCE(dc.token_count, 0),
    'documentTitle', d.title,
    'documentPath', d.path
)
FROM documents d
WHERE dc.document_id = d.id
  AND (dc.metadata IS NULL
       OR dc.metadata->>'versionId' IS NULL
       OR dc.metadata->>'versionId' = '');

-- 2. 建立 metadata JSONB 欄位的 GIN 索引，以加速 JSONB 查詢
CREATE INDEX IF NOT EXISTS idx_document_chunks_metadata
ON document_chunks USING GIN(metadata);

-- 3. 建立針對 versionId 的函數索引，以加速版本過濾查詢
CREATE INDEX IF NOT EXISTS idx_document_chunks_version_id
ON document_chunks ((metadata->>'versionId'));

-- 4. 驗證遷移結果
-- 執行以下查詢確認所有 chunk 都已填入 versionId
-- SELECT COUNT(*) AS total_chunks,
--        COUNT(*) FILTER (WHERE metadata->>'versionId' IS NOT NULL) AS chunks_with_version_id
-- FROM document_chunks;
