# トークン更新 API 作業手順

## 概要
- 目的: クライアントが保持するリフレッシュトークンを使って、新しいアクセストークン（および必要に応じて新しいリフレッシュトークン）を発行する `POST /api/v1/auth/refresh` エンドポイントを実装する。
- 対象リポジトリ: `NaviAI-Back`（Spring Boot / Maven）

## 前提条件
- ログインAPI・ユーザー登録APIは既に実装済みであること。
- 既存の認証ロジック（`AuthController`, `AuthService` 等）を流用する。
- 開発・テストはローカル Maven とコンテナ（`docker-compose.test.yml`）の両方で実行できること。

## API 仕様（提案）
- エンドポイント: `POST /api/v1/auth/refresh`
- 説明: クライアントが保存しているリフレッシュトークンを送信し、有効なら新しいアクセストークンを返す。

### リクエスト
- Content-Type: `application/json`
- ボディ例:

```json
{
  "refreshToken": "<client_refresh_token>"
}
```

### レスポンス
- 成功 (200)

```json
{
  "accessToken": "<new_access_token>",
  "expiresIn": 3600,
  "refreshToken": "<rotated_refresh_token - 任意>"
}
```

- エラー
  - 400: リクエスト不正（トークン欠落など）
  - 401: トークン無効または期限切れ
  - 403/409: その他のポリシー違反（必要に応じて）

## 実装手順（ステップ実行）

1. 既存コードの確認
   - `NaviAI-Back/app/src/main/java` 配下の認証関連ファイルを確認する：`auth/controller/AuthController.java`, `auth/service/AuthServiceImpl.java`, `auth/dto/*` など。
   - ログインで発行しているアクセストークン・リフレッシュトークンの生成方法（JWT 署名・有効期限・クレーム）を把握する。

2. API DTO の追加
   - リクエスト DTO: `RefreshRequest`（`refreshToken` フィールド）
   - レスポンス DTO: `TokenResponse`（`accessToken`, `expiresIn`, `refreshToken` を含める）

3. Service 層のメソッド追加
   - `AuthService` に `refreshTokens(String refreshToken)` を追加（インターフェースがある場合）。
   - 実装 (`AuthServiceImpl`) で次を行う:
     - 受け取ったリフレッシュトークンの検証（署名・期限・DB/ブラックリスト確認など）
     - 有効なら新しいアクセストークンを生成
     - （推奨）リフレッシュトークンのローテーションを行う場合は新しいリフレッシュトークンを発行して保存/無効化処理を実装
     - トークン情報を含む `TokenResponse` を返す

4. Controller の追加/更新
   - `AuthController` に `@PostMapping("/refresh")` を追加。
   - リクエストを受け、`AuthService.refreshTokens()` を呼ぶ。例外は既存の `GlobalExceptionHandler` に任せる。

5. Security 設定の確認
   - `SecurityConfig`（`config` 配下）で `POST /api/v1/auth/refresh` が認可設定でアクセス可能か確認。通常は未認証クライアントがリフレッシュトークンを送るため、該当エンドポイントは `permitAll()` または適切に設定する。

6. 永続化/ブラックリスト（必要に応じて）
    - リフレッシュトークンを永続化して取り消し／ローテーションを実現する場合、下記を実施する。
       - `RefreshToken` エンティティを作成（`app/src/main/java/com/ginga/naviai/auth/entity/RefreshToken.java`）
          - 推奨フィールド: `id`, `user`(ManyToOne), `tokenHash`, `jti`(任意), `createdAt`, `expiresAt`, `lastUsedAt`, `revoked`(boolean), `revokedAt`, `replacedBy`(UUID)
          - トークン本体は平文で保存しない。`tokenHash`(HMAC/SHA256)を格納する設計にすること。
       - `RefreshTokenRepository` を作成（`app/src/main/java/com/ginga/naviai/auth/repository/RefreshTokenRepository.java`）
          - 必要なクエリ: `findByTokenHash`, `findByUserIdAndRevokedFalse`, `deleteExpired` など。
       - マイグレーション SQL を追加（`refresh_tokens` テーブル、インデックス、制約）。DB 設計書への反映を忘れずに。
       - `AuthService` 側での利用:
          - リフレッシュリクエスト受信時は受け取ったトークンをハッシュ化して `RefreshTokenRepository.findByTokenHash` で検証。
          - 有効かつ未 revoked ならアクセストークン発行。ローテーション採用時は新しい `RefreshToken` を生成・保存し、旧トークンを `revoked=true` に更新（`replacedBy` に紐付け）。
          - ログアウト時や管理者による無効化時は対象トークンを `revoked=true` に更新する処理を追加。
       - 運用・パフォーマンス:
          - 照合を高速化するため `token_hash` と `user_id` にインデックスを張る。
          - 期限切れトークンを削除/アーカイブするバッチ（スケジューラ）を用意する。
       - テスト:
          - エンティティ・リポジトリの単体テスト、`AuthService` の振る舞いテスト（有効／無効／ローテーション／不正ケース）を追加する。

7. 例外処理とログ
   - 無効トークン、期限切れ、DB不整合などのケースを想定したカスタム例外（例: `InvalidTokenException`, `TokenExpiredException`）を投げ、`GlobalExceptionHandler` で適切な HTTP ステータスを返す。
   - セキュリティ関連ログは詳細を出し過ぎない（トークン本体はログに出さない）。

8. 単体テストの追加
   - サービス層のテストで以下を確認：有効なリフレッシュトークンで新アクセストークン発行、無効トークンで例外、ローテーション挙動。
   - コントローラの単体テスト（MockMvc 等）でエンドポイントのレスポンスを検証。

## テスト手順

### ローカル（Maven）で実行
1. プロジェクトルートで実行（`app/pom.xml` がある場所）:

```bash
cd NaviAI-Back/app
mvn test -DskipITs
```

### Docker コンテナ内で実行（推奨：環境をコンテナで再現）
1. テスト用 compose を利用してビルド・実行:

```bash
cd NaviAI-Back
docker-compose -f docker-compose.test.yml up --build --abort-on-container-exit
docker-compose -f docker-compose.test.yml down
```

2. 個別にコンテナへ入って Maven 実行する場合（コンテナ名は compose 設定に依存）:

```bash
docker-compose -f docker-compose.test.yml run --rm app mvn -Dtest=AuthServiceTest test
```

## チェックリスト（レビュー用）
- [ ] API 仕様書（上記）と実装が一致している
- [ ] セキュリティ設定が適切（エンドポイントのアクセス制御）
- [ ] リフレッシュトークンの検証ロジックが安全である（署名・期限・取り消し確認）
- [ ] 単体テストが追加され、CI/ローカルで通る
- [ ] ログに敏感情報を出力していない

## 参考ファイル / 参照箇所
- `NaviAI-Back/app/src/main/java/com/ginga/naviai/auth/controller/AuthController.java`（既存のログイン実装）
- `NaviAI-Back/app/src/main/java/com/ginga/naviai/auth/service/AuthServiceImpl.java`
- `NaviAI-Back/app/src/main/java/com/ginga/naviai/config/SecurityConfig.java`
- `NaviAI-Back/docker-compose.test.yml`, `NaviAI-Back/app/Dockerfile.test`

## 補足（設計上の考慮）
- リフレッシュトークンは長期間有効になりがちなので、ローテーション（使用時に新しいリフレッシュトークンを発行し古いものを無効化）を導入するとセキュリティが向上します。
- トークンの保存と取り消し戦略（DB 保存・Redis・Blacklist）を早めに決めて実装すること。

---
作成元: `トークン更新API_作業内容.md` の要件を元に作成。
