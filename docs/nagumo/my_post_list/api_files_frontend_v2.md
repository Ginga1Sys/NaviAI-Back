# 投稿一覧（マイ投稿）API — フロント連携（ブラッシュアップ）

目的  
- フロントのモック（Post 型）に合わせ、基本設計書 (API/DB) に整合したバックエンドファイル一覧・エンドポイント定義とレスポンス仕様を簡潔にまとめる。

Base（設計に準拠）  
- Base URL: /api/v1  
- 認証: Authorization: Bearer <token>（JWT）  
- 共通成功フォーマット: { "data": ..., "meta": { ... } }（基本設計に準拠）

作成・修正ファイル一覧（優先順）
| No | ファイル名 | ディレクトリ | 役割 |
|---:|---|---|---|
|1| Knowledge.java（エンティティ） | app/src/main/java/com/ginga/naviai/knowledge/entity/ | DBテーブルに対応（id, title, body, excerpt, authorId, status, createdAt, publishedAt, thumbnail 等） |
|2| KnowledgeRepository.java | app/src/main/java/com/ginga/naviai/knowledge/repository/ | DBアクセス（author検索、ページング、ソート） |
|3| KnowledgeResponse.java（DTO） | app/src/main/java/com/ginga/naviai/knowledge/dto/ | フロントの Post 型へ変換（id, title, excerpt, date, status, thumbnail） |
|4| KnowledgeService.java | app/src/main/java/com/ginga/naviai/knowledge/service/ | ビジネスロジック定義（例: getMyKnowledge） |
|5| KnowledgeServiceImpl.java | app/src/main/java/com/ginga/naviai/knowledge/service/ | 実装（Repository 呼び出し、DTO 変換、ページング） |
|6| KnowledgeController.java | app/src/main/java/com/ginga/naviai/knowledge/controller/ | エンドポイント実装（認証・パラ検証・レスポンス整形） |
|7| V2__create_knowledge_table.sql | app/src/main/resources/db/migration/ | knowledge テーブル作成マイグレーション（DB 設計に合わせる） |
|8| User.java（修正） | app/src/main/java/com/ginga/naviai/auth/entity/ | User ↔ Knowledge の関連付け（必要に応じて OneToMany） |

推奨エンドポイント（基本設計と整合）
- マイ投稿取得（推奨: サーバ側で JWT からユーザー判定）
  - GET /api/v1/knowledge?mine=true&page=1&per_page=20&sort=created_at
  - 説明: mine=true を指定すると認証ユーザーの投稿を返す（userId パラ不要）
- 代替（管理／参照用）
  - GET /api/v1/knowledge?author_id={id}&page=1&per_page=20

クエリ（例）
- page, per_page（offset/limit）、sort、status（draft,pending,published）

推奨レスポンス（基本設計の共通フォーマット）
- 成功（一覧、ページング情報付き）
{
  "data": [
    {
      "id": "uuid",
      "title": "string",
      "excerpt": "string",
      "date": "YYYY-MM-DD",
      "status": "公開" | "下書き" | "レビュー中" | "差し戻し",
      "thumbnail": "string"
    }
  ],
  "meta": { "page":1, "per_page":20, "total":123 }
}

フロントとのマッピングメモ（必須）
- KnowledgeResponse をフロント Post 型に合わせる
  - createdAt -> date (YYYY-MM-DD)
  - body/summary -> excerpt
  - status: バック側は英語 enum (draft,pending,published,declined) を保持し、API レイヤで日本語ラベルへ変換して返す
- 可能なら共通 API レスポンス形式（data/meta）をフロントで統一的に扱う

実装上のポイント（短）
- 認可: /api/v1/knowledge?mine=true は JWT 検証必須、他ユーザー参照は権限チェック
- ページング: offset/limit（page/per_page）をサポート、最大 per_page=100
- 検索負荷: フルテキストは DB 側の tsvector/Gin を利用（DB 設計に準拠）
- エラー: 基本設計の共通エラーフォーマットを使用

マイグレーション名について
- posts ではなく knowledge を用いる: V2__create_knowledge_table.sql を推奨（ドメイン名に一致させるため）

変更履歴
- v2: 基本設計(API/DB)に整合、エンドポイントとマイグレーション名を knowledge に統一