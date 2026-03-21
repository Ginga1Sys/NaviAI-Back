-- users テーブルに role カラムを追加（RBAC用）
ALTER TABLE users ADD COLUMN IF NOT EXISTS role VARCHAR(50) NOT NULL DEFAULT 'user';

-- knowledge_revision テーブルを追加（編集履歴）
CREATE TABLE IF NOT EXISTS knowledge_revision (
    id BIGSERIAL PRIMARY KEY,
    knowledge_id BIGINT NOT NULL REFERENCES knowledge(id) ON DELETE CASCADE,
    editor_id BIGINT REFERENCES users(id) ON DELETE SET NULL,
    title VARCHAR(500),
    body TEXT,
    diff_summary TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);

-- attachment テーブルを追加（添付ファイル）
CREATE TABLE IF NOT EXISTS attachment (
    id BIGSERIAL PRIMARY KEY,
    knowledge_id BIGINT REFERENCES knowledge(id) ON DELETE CASCADE,
    filename TEXT NOT NULL,
    content_type VARCHAR(100),
    size_bytes BIGINT,
    storage_path TEXT NOT NULL,
    uploaded_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_knowledge_revision_knowledge ON knowledge_revision (knowledge_id, created_at);
CREATE INDEX IF NOT EXISTS idx_attachment_knowledge ON attachment (knowledge_id);
