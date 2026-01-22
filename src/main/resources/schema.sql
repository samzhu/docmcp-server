-- DocMCP Server 資料庫 Schema
-- 此檔案會在應用程式啟動時自動執行

-- 啟用必要的 PostgreSQL 擴充功能
CREATE EXTENSION IF NOT EXISTS vector;
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- 建立 libraries 表（函式庫主表）
CREATE TABLE IF NOT EXISTS libraries (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name VARCHAR(100) NOT NULL UNIQUE,
    display_name VARCHAR(200) NOT NULL,
    description TEXT,
    source_type VARCHAR(20) NOT NULL,
    source_url VARCHAR(500),
    category VARCHAR(50),
    tags TEXT[],
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE libraries IS '儲存函式庫/框架的基本資訊';
COMMENT ON COLUMN libraries.source_type IS '來源類型: GITHUB, LOCAL, MANUAL';

-- 建立 library_versions 表（版本資訊表）
CREATE TABLE IF NOT EXISTS library_versions (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    library_id UUID NOT NULL REFERENCES libraries(id) ON DELETE CASCADE,
    version VARCHAR(50) NOT NULL,
    is_latest BOOLEAN DEFAULT FALSE,
    is_lts BOOLEAN DEFAULT FALSE,
    status VARCHAR(20) DEFAULT 'ACTIVE',
    docs_path VARCHAR(500),
    release_date DATE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(library_id, version)
);

COMMENT ON TABLE library_versions IS '儲存每個函式庫的版本資訊';
COMMENT ON COLUMN library_versions.status IS '版本狀態: ACTIVE, DEPRECATED, EOL';

-- 遷移：為已存在的 library_versions 表添加 is_lts 欄位（使用 IF NOT EXISTS 語法）
ALTER TABLE library_versions ADD COLUMN IF NOT EXISTS is_lts BOOLEAN DEFAULT FALSE;

-- 建立 documents 表（文件表）
CREATE TABLE IF NOT EXISTS documents (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    version_id UUID NOT NULL REFERENCES library_versions(id) ON DELETE CASCADE,
    title VARCHAR(500) NOT NULL,
    path VARCHAR(1000) NOT NULL,
    content TEXT,
    content_hash VARCHAR(64),
    doc_type VARCHAR(50),
    metadata JSONB DEFAULT '{}',
    search_vector TSVECTOR,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(version_id, path)
);

COMMENT ON TABLE documents IS '儲存文件內容和元資料';
COMMENT ON COLUMN documents.search_vector IS '全文檢索向量';

-- 建立 document_chunks 表（文件區塊表，含向量嵌入）
CREATE TABLE IF NOT EXISTS document_chunks (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    document_id UUID NOT NULL REFERENCES documents(id) ON DELETE CASCADE,
    chunk_index INTEGER NOT NULL,
    content TEXT NOT NULL,
    embedding vector(768),
    token_count INTEGER,
    metadata JSONB DEFAULT '{}',
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(document_id, chunk_index)
);

COMMENT ON TABLE document_chunks IS '儲存分塊的文件內容與向量嵌入';
COMMENT ON COLUMN document_chunks.embedding IS '768 維度向量，用於語意搜尋 (text-embedding-004)';

-- 建立 code_examples 表（程式碼範例表）
CREATE TABLE IF NOT EXISTS code_examples (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    document_id UUID NOT NULL REFERENCES documents(id) ON DELETE CASCADE,
    language VARCHAR(50) NOT NULL,
    code TEXT NOT NULL,
    description TEXT,
    start_line INTEGER,
    end_line INTEGER,
    metadata JSONB DEFAULT '{}',
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE code_examples IS '儲存從文件中擷取的程式碼範例';

-- 建立 sync_history 表（同步歷史記錄表）
CREATE TABLE IF NOT EXISTS sync_history (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    version_id UUID NOT NULL REFERENCES library_versions(id) ON DELETE CASCADE,
    status VARCHAR(20) NOT NULL,
    started_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMP WITH TIME ZONE,
    documents_processed INTEGER DEFAULT 0,
    chunks_created INTEGER DEFAULT 0,
    error_message TEXT,
    metadata JSONB DEFAULT '{}'
);

COMMENT ON TABLE sync_history IS '追蹤文件同步歷史記錄';
COMMENT ON COLUMN sync_history.status IS '同步狀態: PENDING, RUNNING, SUCCESS, FAILED';

-- 建立 api_keys 表（API 金鑰表）
CREATE TABLE IF NOT EXISTS api_keys (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name VARCHAR(100) NOT NULL UNIQUE,
    key_hash VARCHAR(255) NOT NULL,
    key_prefix VARCHAR(20) NOT NULL UNIQUE,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    rate_limit INTEGER DEFAULT 1000,
    expires_at TIMESTAMP WITH TIME ZONE,
    last_used_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100)
);

COMMENT ON TABLE api_keys IS '儲存 API 認證金鑰';
COMMENT ON COLUMN api_keys.key_hash IS 'BCrypt 雜湊後的金鑰';
COMMENT ON COLUMN api_keys.key_prefix IS '金鑰前綴，用於識別（如 dmcp_xxxx）';
COMMENT ON COLUMN api_keys.status IS '金鑰狀態: ACTIVE, REVOKED, EXPIRED';

-- 建立索引以優化查詢效能

-- Libraries 索引
CREATE INDEX IF NOT EXISTS idx_libraries_name ON libraries(name);
CREATE INDEX IF NOT EXISTS idx_libraries_category ON libraries(category);
CREATE INDEX IF NOT EXISTS idx_libraries_source_type ON libraries(source_type);

-- Library versions 索引
CREATE INDEX IF NOT EXISTS idx_library_versions_library_id ON library_versions(library_id);
CREATE INDEX IF NOT EXISTS idx_library_versions_is_latest ON library_versions(is_latest) WHERE is_latest = TRUE;
CREATE INDEX IF NOT EXISTS idx_library_versions_is_lts ON library_versions(is_lts) WHERE is_lts = TRUE;
CREATE INDEX IF NOT EXISTS idx_library_versions_status ON library_versions(status);

-- Documents 索引
CREATE INDEX IF NOT EXISTS idx_documents_version_id ON documents(version_id);
CREATE INDEX IF NOT EXISTS idx_documents_doc_type ON documents(doc_type);
CREATE INDEX IF NOT EXISTS idx_documents_search_vector ON documents USING GIN(search_vector);

-- Document chunks 索引
CREATE INDEX IF NOT EXISTS idx_document_chunks_document_id ON document_chunks(document_id);

-- Document chunks metadata 索引（支援 VectorStore filter 機制）
CREATE INDEX IF NOT EXISTS idx_document_chunks_metadata ON document_chunks USING GIN(metadata);
CREATE INDEX IF NOT EXISTS idx_document_chunks_version_id ON document_chunks ((metadata->>'versionId'));

-- Code examples 索引
CREATE INDEX IF NOT EXISTS idx_code_examples_document_id ON code_examples(document_id);
CREATE INDEX IF NOT EXISTS idx_code_examples_language ON code_examples(language);

-- Sync history 索引
CREATE INDEX IF NOT EXISTS idx_sync_history_version_id ON sync_history(version_id);
CREATE INDEX IF NOT EXISTS idx_sync_history_status ON sync_history(status);

-- API keys 索引
CREATE INDEX IF NOT EXISTS idx_api_keys_key_prefix ON api_keys(key_prefix);
CREATE INDEX IF NOT EXISTS idx_api_keys_status ON api_keys(status);

-- 注意：tsvector 觸發器需要在資料庫層面另外設定
-- 因為 Spring SQL 初始化不支援 PostgreSQL 的 $$ 引用語法
-- 可透過 psql 手動執行以下 SQL：
--
-- CREATE OR REPLACE FUNCTION update_documents_search_vector()
-- RETURNS TRIGGER AS $$
-- BEGIN
--     NEW.search_vector :=
--         setweight(to_tsvector('english', COALESCE(NEW.title, '')), 'A') ||
--         setweight(to_tsvector('english', COALESCE(NEW.content, '')), 'B');
--     RETURN NEW;
-- END;
-- $$ LANGUAGE plpgsql;
--
-- CREATE TRIGGER trigger_documents_search_vector
--     BEFORE INSERT OR UPDATE OF title, content
--     ON documents
--     FOR EACH ROW
--     EXECUTE FUNCTION update_documents_search_vector();
