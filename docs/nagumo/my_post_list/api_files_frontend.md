# 投稿一覧（マイ投稿）API — フロント連携用まとめ

## 目的
フロントのモック呼び出し（mockPosts）の型と照らし合わせて、バックエンドで作成するファイル一覧、配置場所、名称、および想定するAPI URLを簡潔にまとめます。

## 作成・修正ファイル一覧

| No | ファイル名 | ディレクトリ | 役割 |
|---:|---|---|---|
| 1 | Post.java | app/src/main/java/com/ginga/naviai/post/entity/ | 投稿エンティティ（id, title, body, excerpt, createdAt, status, thumbnail, authorId） |
| 2 | PostRepository.java | app/src/main/java/com/ginga/naviai/post/repository/ | DBアクセス（ユーザー別取得、ページング） |
| 3 | PostResponse.java | app/src/main/java/com/ginga/naviai/post/dto/ | レスポンスDTO（フロントの Post 型に合わせる） |
| 4 | PostService.java | app/src/main/java/com/ginga/naviai/post/service/ | ビジネスロジック定義（例：getMyPosts） |
| 5 | PostServiceImpl.java | app/src/main/java/com/ginga/naviai/post/service/ | Service実装（Repository呼び出し、DTO変換、ページング） |
| 6 | PostController.java | app/src/main/java/com/ginga/naviai/post/controller/ | HTTPエンドポイント（マイ投稿取得） |
| 7 | V2__create_posts_table.sql | app/src/main/resources/db/migration/ | postsテーブル作成マイグレーション |
| 8 | User.java（修正） | app/src/main/java/com/ginga/naviai/auth/entity/ | 投稿との関連付け（必要に応じて OneToMany） |

## 推奨APIエンドポイント（フロント連携）
- マイ投稿取得（ログインユーザー用）
  - URL: GET /api/my/posts
  - Query: page, size, sort (認証済みユーザーは userId をサーバ側で判定)
  - 代替（管理用／引数渡し）: GET /api/posts?userId={id}&page={page}&size={size}

## リクエスト例
- GET /api/my/posts?page=0&size=20
- GET /api/posts?userId=123&page=0&size=20

## 期待するレスポンス（JSON配列） — フロントの型に合わせる
[
  {
    "id": "string",
    "title": "string",
    "excerpt": "string",
    "date": "YYYY-MM-DD",
    "status": "公開" | "下書き" | "レビュー中" | "差し戻し",
    "thumbnail": "string"
  }
]

## マッピングメモ
- バックエンド DTO (PostResponse) のフィールドをフロント Post 型に合わせる:
  - id -> id
  - title -> title
  - body / summary -> excerpt
  - createdAt -> date (フォーマット YYYY-MM-DD)
  - status -> 日本語表記（Enum もしくは変換ロジック）
  - thumbnail -> thumbnail
- 認可: /api/my/posts は認証トークンからユーザー判定し userId パラ不要とするのが推奨。