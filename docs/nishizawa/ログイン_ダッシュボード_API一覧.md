# ログイン → ダッシュボード 表示に必要なバックエンドAPI一覧

目的: フロントエンド（ログイン画面）からログインし、ダッシュボードを表示するまでに必要なAPIを列挙する。

前提
- ベースパス: `/api/v1`
- 認可: 多くのAPIは `Authorization: Bearer <access_token>` を要求

必須API一覧

1. Auth（認証）
   - `POST /api/v1/auth/login` — ログイン
     - 説明: メール/パスワードで認証し、`access_token` と `refresh_token`（+ expires_in）を返す。
     - リクエスト: `{ "email": "...", "password": "..." }`
     - レスポンス例: `{ "data": { "access_token": "...", "refresh_token": "...", "expires_in": 3600 } }`

   - `POST /api/v1/auth/refresh` — トークン更新
     - 説明: リフレッシュトークンを受け取り新しいアクセストークンを発行する。
     - リクエスト: `{ "refresh_token": "..." }`
     - レスポンス例: `{ "data": { "access_token": "...", "refresh_token": "...", "expires_in": 3600 } }`

    - `POST /api/v1/auth/logout` — ログアウト／リフレッシュ失効 (ダッシュボード表示には必須ではありません)
       - 説明: クライアント側のログアウト時にリフレッシュトークンを無効化する（表示のみを考慮する場合は不要）。
       - リクエスト: `{ "refresh_token": "..." }`（または httpOnly cookie を使う設計により不要）

2. Users（ユーザー情報）
   - `GET /api/v1/users/me` — ログイン中ユーザーのプロファイル取得
     - 説明: ダッシュボード表示前にクライアントが現在のユーザー情報（id, displayName, role, avatar_url, preferences 等）を取得するためのエンドポイント。
     - レスポンス例: `{ "data": { "id": "uuid", "email": "...", "displayName": "...", "role": "USER" } }`

3. Dashboard（ダッシュボード表示用集計）
   - `GET /api/v1/dashboard` — ダッシュボード用サマリデータ
     - 説明: 承認待ち件数、最近の投稿一覧（簡易）、ウィジェット用集計などを返す。ページング/フィルタをサポート可能。
     - レスポンス例: `{ "data": { "pendingCount": 5, "recent": [ ... ], "activity": { ... } } }`

   - `GET /api/v1/dashboard/widgets/{widgetId}` — 個別ウィジェットの詳細（任意）

4. Attachments / Media（画像などの表示）
    - `GET /api/v1/attachments/{id}` — 添付ファイル取得（署名付きURLを返す等）
       - 説明: ダッシュボードで画像やアイコン等を表示する際に利用。アップロード用の`presign`は表示には不要のため削除。

5. Authorization / RBAC（横断的、ミドルウェア）
   - 全APIに対して JWT 検証と `role`/`permissions` によるアクセス制御を行うミドルウェアが必要。
   - 権限違反は `403 Forbidden`、認証失敗は `401 Unauthorized` を返す。

6. Ops（運用・監視）
   - `GET /health` — ヘルスチェック（表示自体には不要だが運用で有用）
   - `GET /metrics` — メトリクス（Prometheus等に対応、表示だけなら不要）

実装上の注意点（短く）
- アクセストークンは短寿命にし、リフレッシュは安全に管理（httpOnly cookie 推奨／DBでの失効管理）。
- ダッシュボード向け集計は負荷が高くなり得るため Redis キャッシュや事前集計を検討する。
- CORS 設定とレート制限をフロントドメインに合わせて調整する。
- 画像等の静的表示は `GET /api/v1/attachments/{id}` で事足りるため、アップロード用 `presign` は不要（表示のみを目的とする場合）。

---
補足: 既にある `POST /api/v1/auth/register`、`POST /api/v1/auth/login`、`POST /api/v1/auth/refresh` は利用前提。上記で足りない点（詳細スキーマや具体的なレスポンス設計）を詰める場合は、各エンドポイントごとのリクエスト/レスポンス仕様案を作成します。