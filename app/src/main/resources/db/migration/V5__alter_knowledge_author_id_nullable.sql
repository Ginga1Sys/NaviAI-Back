-- ユーザー削除時に Knowledge を保持する（author_id を NULL にする）
-- 1. 既存の FK 制約を動的に取得・削除
SET @fk_name = (
    SELECT CONSTRAINT_NAME
    FROM information_schema.KEY_COLUMN_USAGE
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME   = 'knowledge'
      AND COLUMN_NAME  = 'author_id'
      AND REFERENCED_TABLE_NAME = 'users'
    LIMIT 1
);
SET @sql_drop = CONCAT('ALTER TABLE knowledge DROP FOREIGN KEY `', @fk_name, '`');
PREPARE stmt FROM @sql_drop;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 2. author_id を NULL 許容に変更
ALTER TABLE knowledge MODIFY COLUMN author_id BIGINT NULL;

-- 3. ON DELETE SET NULL で FK を再作成（明示的に制約名を付与）
ALTER TABLE knowledge
    ADD CONSTRAINT fk_knowledge_author
        FOREIGN KEY (author_id) REFERENCES users(id) ON DELETE SET NULL;
