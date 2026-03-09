-- knowledge テーブルに excerpt / thumbnail カラムを追加
ALTER TABLE knowledge ADD COLUMN IF NOT EXISTS excerpt VARCHAR(255);
ALTER TABLE knowledge ADD COLUMN IF NOT EXISTS thumbnail VARCHAR(255);
