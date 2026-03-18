-- =============================================
-- V3: knowledge, knowledge_tags, knowledge_moderation, audit_log
-- =============================================

-- knowledge テーブル
CREATE TABLE IF NOT EXISTS knowledge (
    id          VARCHAR(36)   PRIMARY KEY,
    title       VARCHAR(200)  NOT NULL,
    body        TEXT          NOT NULL,
    status      VARCHAR(20)   NOT NULL DEFAULT 'DRAFT',
    category    VARCHAR(100),
    author_id   BIGINT        NOT NULL,
    submitted_at   TIMESTAMP WITH TIME ZONE,
    published_at   TIMESTAMP WITH TIME ZONE,
    declined_reason VARCHAR(2000),
    created_at  TIMESTAMP WITH TIME ZONE DEFAULT now(),
    updated_at  TIMESTAMP WITH TIME ZONE DEFAULT now(),
    CONSTRAINT fk_knowledge_author
        FOREIGN KEY (author_id)
        REFERENCES users(id)
        ON DELETE CASCADE
);

CREATE INDEX idx_knowledge_status    ON knowledge(status);
CREATE INDEX idx_knowledge_author_id ON knowledge(author_id);
CREATE INDEX idx_knowledge_category  ON knowledge(category);

-- knowledge_tags テーブル (Knowledge エンティティの @ElementCollection)
CREATE TABLE IF NOT EXISTS knowledge_tags (
    knowledge_id VARCHAR(36)  NOT NULL,
    tag          VARCHAR(50),
    CONSTRAINT fk_knowledge_tags_knowledge
        FOREIGN KEY (knowledge_id)
        REFERENCES knowledge(id)
        ON DELETE CASCADE
);

CREATE INDEX idx_knowledge_tags_knowledge_id ON knowledge_tags(knowledge_id);

-- knowledge_moderation テーブル
CREATE TABLE IF NOT EXISTS knowledge_moderation (
    knowledge_id  VARCHAR(36)  PRIMARY KEY,
    internal_note TEXT,
    updated_by    BIGINT,
    updated_at    TIMESTAMP WITH TIME ZONE,
    CONSTRAINT fk_knowledge_moderation_knowledge
        FOREIGN KEY (knowledge_id)
        REFERENCES knowledge(id)
        ON DELETE CASCADE
);

-- audit_log テーブル
CREATE TABLE IF NOT EXISTS audit_log (
    id          VARCHAR(36)   PRIMARY KEY,
    action      VARCHAR(80)   NOT NULL,
    actor_id    BIGINT,
    actor_name  VARCHAR(100),
    target_type VARCHAR(30),
    target_id   VARCHAR(36),
    detail_json TEXT,
    created_at  TIMESTAMP WITH TIME ZONE DEFAULT now()
);

CREATE INDEX idx_audit_log_action     ON audit_log(action);
CREATE INDEX idx_audit_log_actor_id   ON audit_log(actor_id);
CREATE INDEX idx_audit_log_created_at ON audit_log(created_at);
