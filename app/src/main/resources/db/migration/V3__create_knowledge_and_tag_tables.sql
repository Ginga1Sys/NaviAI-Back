CREATE TABLE IF NOT EXISTS knowledge (
   id BIGSERIAL PRIMARY KEY,
   author_id BIGINT REFERENCES users(id),
   title VARCHAR(500) NOT NULL DEFAULT '',
   body TEXT,
   status VARCHAR(20) NOT NULL DEFAULT 'draft',
   is_deleted BOOLEAN NOT NULL DEFAULT false,
   created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
   updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
   published_at TIMESTAMPTZ
);

CREATE TABLE IF NOT EXISTS tag (
   id BIGSERIAL PRIMARY KEY,
   name VARCHAR(100) NOT NULL UNIQUE,
   created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS knowledge_tag (
   knowledge_id BIGINT NOT NULL REFERENCES knowledge(id) ON DELETE CASCADE,
   tag_id BIGINT NOT NULL REFERENCES tag(id) ON DELETE CASCADE,
   PRIMARY KEY (knowledge_id, tag_id)
);

CREATE TABLE IF NOT EXISTS comment (
   id BIGSERIAL PRIMARY KEY,
   knowledge_id BIGINT NOT NULL REFERENCES knowledge(id) ON DELETE CASCADE,
   author_id BIGINT REFERENCES users(id),
   body TEXT NOT NULL,
   parent_comment_id BIGINT REFERENCES comment(id) ON DELETE SET NULL,
   is_deleted BOOLEAN NOT NULL DEFAULT false,
   created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS "like" (
   id BIGSERIAL PRIMARY KEY,
   knowledge_id BIGINT NOT NULL REFERENCES knowledge(id) ON DELETE CASCADE,
   user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
   created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
   UNIQUE (knowledge_id, user_id)
);

CREATE INDEX IF NOT EXISTS idx_knowledge_author_created ON knowledge (author_id, created_at);
CREATE INDEX IF NOT EXISTS idx_knowledge_status_published ON knowledge (status, published_at);
CREATE INDEX IF NOT EXISTS idx_tag_name ON tag (name);
