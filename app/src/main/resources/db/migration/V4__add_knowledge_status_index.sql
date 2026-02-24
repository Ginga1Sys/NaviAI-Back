-- status カラムへのインデックス追加（ステータスによるフィルタリングのパフォーマンス向上）
CREATE INDEX idx_knowledge_status ON knowledge (status);
