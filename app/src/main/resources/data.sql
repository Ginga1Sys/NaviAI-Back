-- ============================================================
-- 管理者ユーザーの初期データ
-- パスワード: Admin@1234  (BCrypt cost=10 で生成)
-- ※ WHERE NOT EXISTS により、既に同名ユーザーが存在する場合はスキップ
--   (H2 / PostgreSQL 両対応の構文)
-- ============================================================
INSERT INTO users (username, email, password_hash, display_name, enabled)
SELECT
    'admin',
    'admin@naviai.com',
    '$2b$10$b0UvtPqI3plFe8Is1vr6wOOaGjSXzr.AV2DqVYYPcIiCoEYoHjQ26',
    '管理者',
    TRUE
WHERE NOT EXISTS (
    SELECT 1 FROM users WHERE username = 'admin'
);

-- ============================================================
-- テスト用ユーザー（パスワード: Test@1234）
-- ============================================================
INSERT INTO users (username, email, password_hash, display_name, enabled)
SELECT 'tanaka_taro', 'tanaka@naviai.com',
    '$2b$10$b0UvtPqI3plFe8Is1vr6wOOaGjSXzr.AV2DqVYYPcIiCoEYoHjQ26',
    '田中 太郎', TRUE
WHERE NOT EXISTS (SELECT 1 FROM users WHERE username = 'tanaka_taro');

INSERT INTO users (username, email, password_hash, display_name, enabled)
SELECT 'suzuki_hanako', 'suzuki@naviai.com',
    '$2b$10$b0UvtPqI3plFe8Is1vr6wOOaGjSXzr.AV2DqVYYPcIiCoEYoHjQ26',
    '鈴木 花子', TRUE
WHERE NOT EXISTS (SELECT 1 FROM users WHERE username = 'suzuki_hanako');

INSERT INTO users (username, email, password_hash, display_name, enabled)
SELECT 'yamada_ken', 'yamada@naviai.com',
    '$2b$10$b0UvtPqI3plFe8Is1vr6wOOaGjSXzr.AV2DqVYYPcIiCoEYoHjQ26',
    '山田 健', TRUE
WHERE NOT EXISTS (SELECT 1 FROM users WHERE username = 'yamada_ken');

INSERT INTO users (username, email, password_hash, display_name, enabled)
SELECT 'ito_yuki', 'ito@naviai.com',
    '$2b$10$b0UvtPqI3plFe8Is1vr6wOOaGjSXzr.AV2DqVYYPcIiCoEYoHjQ26',
    '伊藤 結', TRUE
WHERE NOT EXISTS (SELECT 1 FROM users WHERE username = 'ito_yuki');

-- ============================================================
-- テスト用タグ
-- ============================================================
INSERT INTO tag (name, created_at) SELECT 'ChatGPT',                  NOW() WHERE NOT EXISTS (SELECT 1 FROM tag WHERE name = 'ChatGPT');
INSERT INTO tag (name, created_at) SELECT '業務効率化',               NOW() WHERE NOT EXISTS (SELECT 1 FROM tag WHERE name = '業務効率化');
INSERT INTO tag (name, created_at) SELECT 'Python',                   NOW() WHERE NOT EXISTS (SELECT 1 FROM tag WHERE name = 'Python');
INSERT INTO tag (name, created_at) SELECT 'プロンプトエンジニアリング', NOW() WHERE NOT EXISTS (SELECT 1 FROM tag WHERE name = 'プロンプトエンジニアリング');
INSERT INTO tag (name, created_at) SELECT '画像生成AI',               NOW() WHERE NOT EXISTS (SELECT 1 FROM tag WHERE name = '画像生成AI');
INSERT INTO tag (name, created_at) SELECT 'データ分析',               NOW() WHERE NOT EXISTS (SELECT 1 FROM tag WHERE name = 'データ分析');

-- ============================================================
-- テスト用投稿（knowledge）10件
-- ステータス: published、published_at を分散
-- ============================================================

-- 1. ChatGPT × 業務効率化
INSERT INTO knowledge (author_id, title, body, status, is_deleted, published_at, created_at, updated_at)
SELECT
    (SELECT id FROM users WHERE username = 'tanaka_taro'),
    'ChatGPTを使った議事録自動生成の実践例',
    '会議の録音データをWhisperで文字起こしし、ChatGPTで議事録フォーマットに整形する手順を紹介します。導入後、議事録作成時間を約80%削減できました。',
    'published',
    FALSE,
    TIMESTAMPADD(DAY, -60, NOW()),
    TIMESTAMPADD(DAY, -61, NOW()),
    TIMESTAMPADD(DAY, -60, NOW())
WHERE NOT EXISTS (SELECT 1 FROM knowledge WHERE title = 'ChatGPTを使った議事録自動生成の実践例');

-- 2. プロンプトエンジニアリング
INSERT INTO knowledge (author_id, title, body, status, is_deleted, published_at, created_at, updated_at)
SELECT
    (SELECT id FROM users WHERE username = 'suzuki_hanako'),
    'プロンプトエンジニアリング入門：精度を上げる5つのテクニック',
    'AIへのプロンプト設計は出力品質に直結します。Few-shot学習・役割設定・Chain-of-thoughtなど、すぐ使えるテクニックをまとめました。',
    'published',
    FALSE,
    TIMESTAMPADD(DAY, -50, NOW()),
    TIMESTAMPADD(DAY, -51, NOW()),
    TIMESTAMPADD(DAY, -50, NOW())
WHERE NOT EXISTS (SELECT 1 FROM knowledge WHERE title = 'プロンプトエンジニアリング入門：精度を上げる5つのテクニック');

-- 3. Python × データ分析
INSERT INTO knowledge (author_id, title, body, status, is_deleted, published_at, created_at, updated_at)
SELECT
    (SELECT id FROM users WHERE username = 'yamada_ken'),
    'PandasとChatGPT APIで売上データ分析を自動化する',
    'Pandasで加工した売上CSVをChatGPT APIに投げてインサイト抽出するPythonスクリプトのサンプルを共有します。月次レポートの自動化に活用できます。',
    'published',
    FALSE,
    TIMESTAMPADD(DAY, -45, NOW()),
    TIMESTAMPADD(DAY, -46, NOW()),
    TIMESTAMPADD(DAY, -45, NOW())
WHERE NOT EXISTS (SELECT 1 FROM knowledge WHERE title = 'PandasとChatGPT APIで売上データ分析を自動化する');

-- 4. 画像生成AI
INSERT INTO knowledge (author_id, title, body, status, is_deleted, published_at, created_at, updated_at)
SELECT
    (SELECT id FROM users WHERE username = 'tanaka_taro'),
    'Stable Diffusionで社内資料のアイキャッチ画像を量産する方法',
    '商用利用可能なライセンスのStable Diffusionモデルを社内環境に構築し、プレゼン資料用アイキャッチを効率的に生成するワークフローを紹介します。',
    'published',
    FALSE,
    TIMESTAMPADD(DAY, -38, NOW()),
    TIMESTAMPADD(DAY, -39, NOW()),
    TIMESTAMPADD(DAY, -38, NOW())
WHERE NOT EXISTS (SELECT 1 FROM knowledge WHERE title = 'Stable Diffusionで社内資料のアイキャッチ画像を量産する方法');

-- 5. ChatGPT × プロンプトエンジニアリング
INSERT INTO knowledge (author_id, title, body, status, is_deleted, published_at, created_at, updated_at)
SELECT
    (SELECT id FROM users WHERE username = 'suzuki_hanako'),
    'ChatGPT APIを使ったカスタマーサポートBot構築ガイド',
    'よくある問い合わせ対応をChatGPT APIで自動化する実装例を紹介。RAGパターンによる社内FAQとの連携方法も解説します。',
    'published',
    FALSE,
    TIMESTAMPADD(DAY, -30, NOW()),
    TIMESTAMPADD(DAY, -31, NOW()),
    TIMESTAMPADD(DAY, -30, NOW())
WHERE NOT EXISTS (SELECT 1 FROM knowledge WHERE title = 'ChatGPT APIを使ったカスタマーサポートBot構築ガイド');

-- 6. Python × 業務効率化
INSERT INTO knowledge (author_id, title, body, status, is_deleted, published_at, created_at, updated_at)
SELECT
    (SELECT id FROM users WHERE username = 'yamada_ken'),
    'PythonスクリプトとGitHub Actionsで週次レポートを全自動化',
    'データ収集・集計・メール送信までをPython + GitHub Actionsで完全自動化するCI/CDパイプラインの構築手順を解説します。',
    'published',
    FALSE,
    TIMESTAMPADD(DAY, -22, NOW()),
    TIMESTAMPADD(DAY, -23, NOW()),
    TIMESTAMPADD(DAY, -22, NOW())
WHERE NOT EXISTS (SELECT 1 FROM knowledge WHERE title = 'PythonスクリプトとGitHub Actionsで週次レポートを全自動化');

-- 7. データ分析 × 画像生成AI
INSERT INTO knowledge (author_id, title, body, status, is_deleted, published_at, created_at, updated_at)
SELECT
    (SELECT id FROM users WHERE username = 'admin'),
    'AIを活用したデータビジュアライゼーションの最前線',
    'Matplotlibで生成したグラフをAIが解説文と一緒に自動レポート化する取り組みを紹介。経営層への報告資料作成効率が大幅に向上しました。',
    'published',
    FALSE,
    TIMESTAMPADD(DAY, -15, NOW()),
    TIMESTAMPADD(DAY, -16, NOW()),
    TIMESTAMPADD(DAY, -15, NOW())
WHERE NOT EXISTS (SELECT 1 FROM knowledge WHERE title = 'AIを活用したデータビジュアライゼーションの最前線');

-- 8. 業務効率化 × ChatGPT
INSERT INTO knowledge (author_id, title, body, status, is_deleted, published_at, created_at, updated_at)
SELECT
    (SELECT id FROM users WHERE username = 'tanaka_taro'),
    'メール文章の下書きをChatGPTで3分以内に仕上げるフロー',
    '顧客向けメールの下書き作成にChatGPTを活用するプロンプトテンプレートと、Outlookマクロとの連携方法をまとめました。',
    'published',
    FALSE,
    TIMESTAMPADD(DAY, -10, NOW()),
    TIMESTAMPADD(DAY, -11, NOW()),
    TIMESTAMPADD(DAY, -10, NOW())
WHERE NOT EXISTS (SELECT 1 FROM knowledge WHERE title = 'メール文章の下書きをChatGPTで3分以内に仕上げるフロー');

-- 9. プロンプトエンジニアリング × Python
INSERT INTO knowledge (author_id, title, body, status, is_deleted, published_at, created_at, updated_at)
SELECT
    (SELECT id FROM users WHERE username = 'suzuki_hanako'),
    'LangChainでプロンプトチェーンを組み業務自動化ボットを作る',
    'LangChainのChainクラスを利用して複数ステップのプロンプトを連結し、情報収集から要約・回答生成まで一気通貫で処理する実装例を紹介します。',
    'published',
    FALSE,
    TIMESTAMPADD(DAY, -5, NOW()),
    TIMESTAMPADD(DAY, -6, NOW()),
    TIMESTAMPADD(DAY, -5, NOW())
WHERE NOT EXISTS (SELECT 1 FROM knowledge WHERE title = 'LangChainでプロンプトチェーンを組み業務自動化ボットを作る');

-- 10. 画像生成AI × 業務効率化
INSERT INTO knowledge (author_id, title, body, status, is_deleted, published_at, created_at, updated_at)
SELECT
    (SELECT id FROM users WHERE username = 'yamada_ken'),
    'DALL-E 3でプレゼン資料のイラストを瞬時に生成するTips',
    'ChatGPT Plus / DALL-E 3 APIを使い、プレゼンのコンセプトに合ったイラストをプロンプト1行で生成するベストプラクティスをまとめました。',
    'published',
    FALSE,
    TIMESTAMPADD(DAY, -2, NOW()),
    TIMESTAMPADD(DAY, -3, NOW()),
    TIMESTAMPADD(DAY, -2, NOW())
WHERE NOT EXISTS (SELECT 1 FROM knowledge WHERE title = 'DALL-E 3でプレゼン資料のイラストを瞬時に生成するTips');

-- ============================================================
-- テスト用タグ紐付け（knowledge_tag）
-- ============================================================
INSERT INTO knowledge_tag (knowledge_id, tag_id)
SELECT k.id, t.id FROM knowledge k, tag t
WHERE k.title = 'ChatGPTを使った議事録自動生成の実践例'         AND t.name = 'ChatGPT'
  AND NOT EXISTS (SELECT 1 FROM knowledge_tag kt WHERE kt.knowledge_id = k.id AND kt.tag_id = t.id);

INSERT INTO knowledge_tag (knowledge_id, tag_id)
SELECT k.id, t.id FROM knowledge k, tag t
WHERE k.title = 'ChatGPTを使った議事録自動生成の実践例'         AND t.name = '業務効率化'
  AND NOT EXISTS (SELECT 1 FROM knowledge_tag kt WHERE kt.knowledge_id = k.id AND kt.tag_id = t.id);

INSERT INTO knowledge_tag (knowledge_id, tag_id)
SELECT k.id, t.id FROM knowledge k, tag t
WHERE k.title = 'プロンプトエンジニアリング入門：精度を上げる5つのテクニック' AND t.name = 'プロンプトエンジニアリング'
  AND NOT EXISTS (SELECT 1 FROM knowledge_tag kt WHERE kt.knowledge_id = k.id AND kt.tag_id = t.id);

INSERT INTO knowledge_tag (knowledge_id, tag_id)
SELECT k.id, t.id FROM knowledge k, tag t
WHERE k.title = 'プロンプトエンジニアリング入門：精度を上げる5つのテクニック' AND t.name = 'ChatGPT'
  AND NOT EXISTS (SELECT 1 FROM knowledge_tag kt WHERE kt.knowledge_id = k.id AND kt.tag_id = t.id);

INSERT INTO knowledge_tag (knowledge_id, tag_id)
SELECT k.id, t.id FROM knowledge k, tag t
WHERE k.title = 'PandasとChatGPT APIで売上データ分析を自動化する' AND t.name = 'Python'
  AND NOT EXISTS (SELECT 1 FROM knowledge_tag kt WHERE kt.knowledge_id = k.id AND kt.tag_id = t.id);

INSERT INTO knowledge_tag (knowledge_id, tag_id)
SELECT k.id, t.id FROM knowledge k, tag t
WHERE k.title = 'PandasとChatGPT APIで売上データ分析を自動化する' AND t.name = 'データ分析'
  AND NOT EXISTS (SELECT 1 FROM knowledge_tag kt WHERE kt.knowledge_id = k.id AND kt.tag_id = t.id);

INSERT INTO knowledge_tag (knowledge_id, tag_id)
SELECT k.id, t.id FROM knowledge k, tag t
WHERE k.title = 'Stable Diffusionで社内資料のアイキャッチ画像を量産する方法' AND t.name = '画像生成AI'
  AND NOT EXISTS (SELECT 1 FROM knowledge_tag kt WHERE kt.knowledge_id = k.id AND kt.tag_id = t.id);

INSERT INTO knowledge_tag (knowledge_id, tag_id)
SELECT k.id, t.id FROM knowledge k, tag t
WHERE k.title = 'ChatGPT APIを使ったカスタマーサポートBot構築ガイド' AND t.name = 'ChatGPT'
  AND NOT EXISTS (SELECT 1 FROM knowledge_tag kt WHERE kt.knowledge_id = k.id AND kt.tag_id = t.id);

INSERT INTO knowledge_tag (knowledge_id, tag_id)
SELECT k.id, t.id FROM knowledge k, tag t
WHERE k.title = 'ChatGPT APIを使ったカスタマーサポートBot構築ガイド' AND t.name = 'プロンプトエンジニアリング'
  AND NOT EXISTS (SELECT 1 FROM knowledge_tag kt WHERE kt.knowledge_id = k.id AND kt.tag_id = t.id);

INSERT INTO knowledge_tag (knowledge_id, tag_id)
SELECT k.id, t.id FROM knowledge k, tag t
WHERE k.title = 'PythonスクリプトとGitHub Actionsで週次レポートを全自動化' AND t.name = 'Python'
  AND NOT EXISTS (SELECT 1 FROM knowledge_tag kt WHERE kt.knowledge_id = k.id AND kt.tag_id = t.id);

INSERT INTO knowledge_tag (knowledge_id, tag_id)
SELECT k.id, t.id FROM knowledge k, tag t
WHERE k.title = 'PythonスクリプトとGitHub Actionsで週次レポートを全自動化' AND t.name = '業務効率化'
  AND NOT EXISTS (SELECT 1 FROM knowledge_tag kt WHERE kt.knowledge_id = k.id AND kt.tag_id = t.id);

INSERT INTO knowledge_tag (knowledge_id, tag_id)
SELECT k.id, t.id FROM knowledge k, tag t
WHERE k.title = 'AIを活用したデータビジュアライゼーションの最前線' AND t.name = 'データ分析'
  AND NOT EXISTS (SELECT 1 FROM knowledge_tag kt WHERE kt.knowledge_id = k.id AND kt.tag_id = t.id);

INSERT INTO knowledge_tag (knowledge_id, tag_id)
SELECT k.id, t.id FROM knowledge k, tag t
WHERE k.title = 'AIを活用したデータビジュアライゼーションの最前線' AND t.name = '画像生成AI'
  AND NOT EXISTS (SELECT 1 FROM knowledge_tag kt WHERE kt.knowledge_id = k.id AND kt.tag_id = t.id);

INSERT INTO knowledge_tag (knowledge_id, tag_id)
SELECT k.id, t.id FROM knowledge k, tag t
WHERE k.title = 'メール文章の下書きをChatGPTで3分以内に仕上げるフロー' AND t.name = 'ChatGPT'
  AND NOT EXISTS (SELECT 1 FROM knowledge_tag kt WHERE kt.knowledge_id = k.id AND kt.tag_id = t.id);

INSERT INTO knowledge_tag (knowledge_id, tag_id)
SELECT k.id, t.id FROM knowledge k, tag t
WHERE k.title = 'メール文章の下書きをChatGPTで3分以内に仕上げるフロー' AND t.name = '業務効率化'
  AND NOT EXISTS (SELECT 1 FROM knowledge_tag kt WHERE kt.knowledge_id = k.id AND kt.tag_id = t.id);

INSERT INTO knowledge_tag (knowledge_id, tag_id)
SELECT k.id, t.id FROM knowledge k, tag t
WHERE k.title = 'LangChainでプロンプトチェーンを組み業務自動化ボットを作る' AND t.name = 'プロンプトエンジニアリング'
  AND NOT EXISTS (SELECT 1 FROM knowledge_tag kt WHERE kt.knowledge_id = k.id AND kt.tag_id = t.id);

INSERT INTO knowledge_tag (knowledge_id, tag_id)
SELECT k.id, t.id FROM knowledge k, tag t
WHERE k.title = 'LangChainでプロンプトチェーンを組み業務自動化ボットを作る' AND t.name = 'Python'
  AND NOT EXISTS (SELECT 1 FROM knowledge_tag kt WHERE kt.knowledge_id = k.id AND kt.tag_id = t.id);

INSERT INTO knowledge_tag (knowledge_id, tag_id)
SELECT k.id, t.id FROM knowledge k, tag t
WHERE k.title = 'DALL-E 3でプレゼン資料のイラストを瞬時に生成するTips' AND t.name = '画像生成AI'
  AND NOT EXISTS (SELECT 1 FROM knowledge_tag kt WHERE kt.knowledge_id = k.id AND kt.tag_id = t.id);

INSERT INTO knowledge_tag (knowledge_id, tag_id)
SELECT k.id, t.id FROM knowledge k, tag t
WHERE k.title = 'DALL-E 3でプレゼン資料のイラストを瞬時に生成するTips' AND t.name = '業務効率化'
  AND NOT EXISTS (SELECT 1 FROM knowledge_tag kt WHERE kt.knowledge_id = k.id AND kt.tag_id = t.id);

-- ============================================================
-- テスト用いいね（like）- 投稿ごとに分散
-- ============================================================

-- like テーブルが存在しない場合に作成（JPA エンティティ未定義のため）
CREATE TABLE IF NOT EXISTS "like" (
    id         BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    knowledge_id BIGINT NOT NULL REFERENCES knowledge(id) ON DELETE CASCADE,
    user_id    BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    UNIQUE (knowledge_id, user_id)
);

-- ============================================================
-- コメントテーブル（テストデータ用）。
-- Dashboard のアクティビティ集計でコメント数を日別集計するために最低限のカラムを用意する。
-- JPA エンティティは未定義のため、まずはスキーマだけ用意する。
-- ============================================================
CREATE TABLE IF NOT EXISTS comment (
  id BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
  knowledge_id BIGINT NOT NULL REFERENCES knowledge(id) ON DELETE CASCADE,
  user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  body TEXT,
  is_deleted BOOLEAN DEFAULT FALSE,
  created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- 投稿1: 3いいね（suzuki_hanako, yamada_ken, admin）
INSERT INTO "like" (knowledge_id, user_id)
SELECT k.id, u.id FROM knowledge k, users u
WHERE k.title = 'ChatGPTを使った議事録自動生成の実践例' AND u.username = 'suzuki_hanako'
  AND NOT EXISTS (SELECT 1 FROM "like" l WHERE l.knowledge_id = k.id AND l.user_id = u.id);

INSERT INTO "like" (knowledge_id, user_id)
SELECT k.id, u.id FROM knowledge k, users u
WHERE k.title = 'ChatGPTを使った議事録自動生成の実践例' AND u.username = 'yamada_ken'
  AND NOT EXISTS (SELECT 1 FROM "like" l WHERE l.knowledge_id = k.id AND l.user_id = u.id);

INSERT INTO "like" (knowledge_id, user_id)
SELECT k.id, u.id FROM knowledge k, users u
WHERE k.title = 'ChatGPTを使った議事録自動生成の実践例' AND u.username = 'admin'
  AND NOT EXISTS (SELECT 1 FROM "like" l WHERE l.knowledge_id = k.id AND l.user_id = u.id);

-- 投稿2: 5いいね（tanaka_taro, yamada_ken, admin + admin除いて2件追加は別ユーザーがないため3件）
INSERT INTO "like" (knowledge_id, user_id)
SELECT k.id, u.id FROM knowledge k, users u
WHERE k.title = 'プロンプトエンジニアリング入門：精度を上げる5つのテクニック' AND u.username = 'tanaka_taro'
  AND NOT EXISTS (SELECT 1 FROM "like" l WHERE l.knowledge_id = k.id AND l.user_id = u.id);

INSERT INTO "like" (knowledge_id, user_id)
SELECT k.id, u.id FROM knowledge k, users u
WHERE k.title = 'プロンプトエンジニアリング入門：精度を上げる5つのテクニック' AND u.username = 'yamada_ken'
  AND NOT EXISTS (SELECT 1 FROM "like" l WHERE l.knowledge_id = k.id AND l.user_id = u.id);

INSERT INTO "like" (knowledge_id, user_id)
SELECT k.id, u.id FROM knowledge k, users u
WHERE k.title = 'プロンプトエンジニアリング入門：精度を上げる5つのテクニック' AND u.username = 'admin'
  AND NOT EXISTS (SELECT 1 FROM "like" l WHERE l.knowledge_id = k.id AND l.user_id = u.id);

-- 投稿3: 2いいね
INSERT INTO "like" (knowledge_id, user_id)
SELECT k.id, u.id FROM knowledge k, users u
WHERE k.title = 'PandasとChatGPT APIで売上データ分析を自動化する' AND u.username = 'tanaka_taro'
  AND NOT EXISTS (SELECT 1 FROM "like" l WHERE l.knowledge_id = k.id AND l.user_id = u.id);

INSERT INTO "like" (knowledge_id, user_id)
SELECT k.id, u.id FROM knowledge k, users u
WHERE k.title = 'PandasとChatGPT APIで売上データ分析を自動化する' AND u.username = 'suzuki_hanako'
  AND NOT EXISTS (SELECT 1 FROM "like" l WHERE l.knowledge_id = k.id AND l.user_id = u.id);

-- 投稿4: 1いいね
INSERT INTO "like" (knowledge_id, user_id)
SELECT k.id, u.id FROM knowledge k, users u
WHERE k.title = 'Stable Diffusionで社内資料のアイキャッチ画像を量産する方法' AND u.username = 'suzuki_hanako'
  AND NOT EXISTS (SELECT 1 FROM "like" l WHERE l.knowledge_id = k.id AND l.user_id = u.id);

-- 投稿5: 5いいね
INSERT INTO "like" (knowledge_id, user_id)
SELECT k.id, u.id FROM knowledge k, users u
WHERE k.title = 'ChatGPT APIを使ったカスタマーサポートBot構築ガイド' AND u.username = 'tanaka_taro'
  AND NOT EXISTS (SELECT 1 FROM "like" l WHERE l.knowledge_id = k.id AND l.user_id = u.id);

INSERT INTO "like" (knowledge_id, user_id)
SELECT k.id, u.id FROM knowledge k, users u
WHERE k.title = 'ChatGPT APIを使ったカスタマーサポートBot構築ガイド' AND u.username = 'yamada_ken'
  AND NOT EXISTS (SELECT 1 FROM "like" l WHERE l.knowledge_id = k.id AND l.user_id = u.id);

INSERT INTO "like" (knowledge_id, user_id)
SELECT k.id, u.id FROM knowledge k, users u
WHERE k.title = 'ChatGPT APIを使ったカスタマーサポートBot構築ガイド' AND u.username = 'admin'
  AND NOT EXISTS (SELECT 1 FROM "like" l WHERE l.knowledge_id = k.id AND l.user_id = u.id);

INSERT INTO "like" (knowledge_id, user_id)
SELECT k.id, u.id FROM knowledge k, users u
WHERE k.title = 'ChatGPT APIを使ったカスタマーサポートBot構築ガイド' AND u.username = 'suzuki_hanako'
  AND NOT EXISTS (SELECT 1 FROM "like" l WHERE l.knowledge_id = k.id AND l.user_id = u.id);

INSERT INTO "like" (knowledge_id, user_id)
SELECT k.id, u.id FROM knowledge k, users u
WHERE k.title = 'ChatGPT APIを使ったカスタマーサポートBot構築ガイド' AND u.username = 'ito_yuki'
  AND NOT EXISTS (SELECT 1 FROM "like" l WHERE l.knowledge_id = k.id AND l.user_id = u.id);

-- 投稿6: 2いいね
INSERT INTO "like" (knowledge_id, user_id)
SELECT k.id, u.id FROM knowledge k, users u
WHERE k.title = 'PythonスクリプトとGitHub Actionsで週次レポートを全自動化' AND u.username = 'tanaka_taro'
  AND NOT EXISTS (SELECT 1 FROM "like" l WHERE l.knowledge_id = k.id AND l.user_id = u.id);

INSERT INTO "like" (knowledge_id, user_id)
SELECT k.id, u.id FROM knowledge k, users u
WHERE k.title = 'PythonスクリプトとGitHub Actionsで週次レポートを全自動化' AND u.username = 'admin'
  AND NOT EXISTS (SELECT 1 FROM "like" l WHERE l.knowledge_id = k.id AND l.user_id = u.id);

-- 投稿7: 3いいね
INSERT INTO "like" (knowledge_id, user_id)
SELECT k.id, u.id FROM knowledge k, users u
WHERE k.title = 'AIを活用したデータビジュアライゼーションの最前線' AND u.username = 'tanaka_taro'
  AND NOT EXISTS (SELECT 1 FROM "like" l WHERE l.knowledge_id = k.id AND l.user_id = u.id);

INSERT INTO "like" (knowledge_id, user_id)
SELECT k.id, u.id FROM knowledge k, users u
WHERE k.title = 'AIを活用したデータビジュアライゼーションの最前線' AND u.username = 'suzuki_hanako'
  AND NOT EXISTS (SELECT 1 FROM "like" l WHERE l.knowledge_id = k.id AND l.user_id = u.id);

INSERT INTO "like" (knowledge_id, user_id)
SELECT k.id, u.id FROM knowledge k, users u
WHERE k.title = 'AIを活用したデータビジュアライゼーションの最前線' AND u.username = 'yamada_ken'
  AND NOT EXISTS (SELECT 1 FROM "like" l WHERE l.knowledge_id = k.id AND l.user_id = u.id);

-- 投稿8: 1いいね
INSERT INTO "like" (knowledge_id, user_id)
SELECT k.id, u.id FROM knowledge k, users u
WHERE k.title = 'メール文章の下書きをChatGPTで3分以内に仕上げるフロー' AND u.username = 'yamada_ken'
  AND NOT EXISTS (SELECT 1 FROM "like" l WHERE l.knowledge_id = k.id AND l.user_id = u.id);

-- 投稿9: 2いいね
INSERT INTO "like" (knowledge_id, user_id)
SELECT k.id, u.id FROM knowledge k, users u
WHERE k.title = 'LangChainでプロンプトチェーンを組み業務自動化ボットを作る' AND u.username = 'tanaka_taro'
  AND NOT EXISTS (SELECT 1 FROM "like" l WHERE l.knowledge_id = k.id AND l.user_id = u.id);

INSERT INTO "like" (knowledge_id, user_id)
SELECT k.id, u.id FROM knowledge k, users u
WHERE k.title = 'LangChainでプロンプトチェーンを組み業務自動化ボットを作る' AND u.username = 'admin'
  AND NOT EXISTS (SELECT 1 FROM "like" l WHERE l.knowledge_id = k.id AND l.user_id = u.id);

-- 投稿10: 0いいね（いいねなし）
