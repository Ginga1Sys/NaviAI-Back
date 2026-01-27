# ユーザー登録API 作業手順

作成日: 2026-01-24

対象: バックエンド (Spring Boot) のユーザー登録API

エンドポイント: `/api/v1/auth/register`

## 1. 概要
- 目的: 新規ユーザーを登録するAPIの設計・実装・検証手順をまとめる。
- 対象リポジトリ: `NaviAI-Back/app`（Mavenプロジェクト）

## 2. 前提・前提環境
- Java 11以上（プロジェクトで指定されたバージョンに従う）
- Maven ビルド: `mvn` を利用（`NaviAI-Back/app/pom.xml` を参照）
- DB: 開発はローカルのPostgreSQL（またはプロジェクトで指定のDB）を想定
- 環境変数/設定:
  - `SPRING_DATASOURCE_URL`
  - `SPRING_DATASOURCE_USERNAME`
  - `SPRING_DATASOURCE_PASSWORD`
  - `MAIL_HOST`, `MAIL_PORT`, `MAIL_USERNAME`, `MAIL_PASSWORD`（メール送信が必要な場合）

## 3. 要求仕様（API仕様）
- HTTPメソッド: `POST`
- パス: `/api/v1/auth/register`
- ヘッダー:
  - `Content-Type: application/json`
- リクエストボディ（JSON）:

```json
{
  "username": "user01",
  "email": "example@ginga.info",
  "password": "P@ssw0rd",
  "displayName": "山田 太郎"
}
```

- フィールド要件:
  - `username`: 必須、英数字と一部記号、最小3文字・最大30文字、ユニーク
  - `email`: 必須、メール形式（@ginga.infoドメイン限定）、ユニーク
  - `password`: 必須、最小8文字、強度チェック（英大文字・英小文字・数字・記号のうち少なくとも3種）
  - `displayName`: 任意、最大100文字

- 成功レスポンス:
  - ステータス: `201 Created`
  - ボディ例:

```json
{
  "id": 123,
  "username": "user01",
  "email": "example@ginga.info",
  "displayName": "山田 太郎",
  "createdAt": "2026-01-24T12:34:56Z"
}
```

- エラーケース（例）:
  - `400 Bad Request` : バリデーションエラー（詳細を配列で返す）
  - `409 Conflict` : `username` または `email` が既に存在
  - `500 Internal Server Error` : サーバー側の予期せぬエラー

## 4. DB設計（ユーザー基礎テーブル）
- テーブル: `users`

推奨カラム:

```sql
CREATE TABLE users (
  id BIGSERIAL PRIMARY KEY,
  username VARCHAR(30) NOT NULL UNIQUE,
  email VARCHAR(254) NOT NULL UNIQUE,
  password_hash VARCHAR(255) NOT NULL,
  display_name VARCHAR(100),
  enabled BOOLEAN NOT NULL DEFAULT TRUE,
  created_at TIMESTAMP WITH TIME ZONE DEFAULT now(),
  updated_at TIMESTAMP WITH TIME ZONE DEFAULT now()
);
```

注: パスワードはハッシュで保存する。`password_hash` はbcryptなどの結果を格納する長さを確保する。

## 5. マイグレーション
- プロジェクトでFlywayやLiquibaseを使っている場合はそれに合わせる。
- 例（Flyway用 SQL ファイル）: `V1__create_users_table.sql` に上記 `CREATE TABLE` を配置。

## 6. 実装手順（推奨構成: Controller → Service → Repository）

1) DTO / リクエスト/レスポンス定義
  - `RegisterRequest` (username, email, password, displayName)
  - `UserResponse` (id, username, email, displayName, createdAt)

2) Entity定義
  - `User` エンティティ: `users` テーブルにマッピング

3) Repository
  - `UserRepository extends JpaRepository<User, Long>`
  - メソッド: `Optional<User> findByUsername(String username)`、`Optional<User> findByEmail(String email)`

4) Service 層
  - `AuthService.register(RegisterRequest req)`
  - 処理フロー:
    a. 入力バリデーション（serviceでも再確認）
    b. `username`/`email` の重複チェック
    c. パスワードハッシュ化（`BCryptPasswordEncoder` を利用）
    d. `User` エンティティを保存
    e. 必要であれば確認メール発行（非同期キュー／メール送信）
    f. `UserResponse` を返す

5) Controller 層
  - `@PostMapping("/api/v1/auth/register")` ハンドラ
  - 受け取った `RegisterRequest` を `@Valid` で検証
  - 成功時は `ResponseEntity.status(HttpStatus.CREATED).body(userResponse)` を返す

6) バリデーション
  - Bean Validation (`@NotBlank`, `@Email`, `@Size`) を使用
  - カスタムパスワードバリデータ（正規表現またはカスタムアノテーション）を導入

7) 例外ハンドリング
  - 共通の `@ControllerAdvice` を用意し、`MethodArgumentNotValidException` を `400` に変換
  - 重複時はカスタム例外 `DuplicateResourceException` を投げて `409` を返す

8) セキュリティ
  - パスワード保存: `BCryptPasswordEncoder`（強度パラメータを設定）
  - レートリミット: 登録APIはブルートフォース対策として適切なレート制限を設ける（APIゲートウェイかフィルタで）
  - メール確認: 登録後は `enabled=false` にして確認メールのトークンで有効化する方式を推奨

## 7. テスト手順

1) 単体テスト (Service層)
  - `AuthService.register` のユニットテスト
  - モック: `UserRepository`、`PasswordEncoder`、`MailService` 等
  - ケース: 正常登録、username重複、email重複、弱パスワード

2) 統合テスト (Controller)
  - `@SpringBootTest` と `@AutoConfigureMockMvc` を用いて `MockMvc` でエンドポイントを検証
  - DBはH2のインメモリを使用するか、TestcontainersでPostgresを起動
  - ケース: 正常フローで201を返すこと、バリデーションエラーで400を返すこと、重複で409を返すこと

3) E2E（必要なら）
  - フロントエンドとの連携を確認する場合は、ローカル環境でバックエンド起動後にcurlやPlaywrightで確認

コマンド例（プロジェクトルートが `NaviAI-Back/app` の場合）:

```bash
# 単体テスト/統合テスト
mvn -f app/pom.xml clean test

# アプリ起動（ローカル）
mvn -f app/pom.xml spring-boot:run
```

## 8. ローカル検証（手順）
1. DB接続情報を環境変数または `application-dev.yml` に設定する（開発用DB）
2. マイグレーションを適用する（Flyway等を使用している場合）
3. アプリを起動
4. 登録リクエストを送信（curl例）:

```bash
curl -X POST http://localhost:8080/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{"username":"user01","email":"user@example.com","password":"P@ssw0rd","displayName":"山田 太郎"}'
```

5. DB上にユーザーが作成されているか確認

## 9. ロギング・監視
- 重要イベント（登録成功、重複検知、エラー）をINFO/WARN/ERRORで記録
- 個人情報（パスワード）はログに出力しない
- 監査ログが必要なら別テーブル（`user_audit`）へ登録する設計を検討

## 10. エラーレスポンス設計（例）

```json
{
  "timestamp": "2026-01-24T12:00:00Z",
  "status": 400,
  "errors": [
    {"field":"email","message":"メールアドレスの形式が不正です"},
    {"field":"password","message":"パスワードは8文字以上である必要があります"}
  ]
}
```

## 11. セキュリティ上の注意点
- 平文パスワードを送らせる場合はTLS必須（常時HTTPS運用）
- パスワードハッシュは`BCrypt`推奨（ソルトは自動付与）
- Eメール確認とCAPTCHAの導入を検討（ボット登録防止）
- メール送信に失敗した場合は再試行キューへ入れる

## 12. デプロイ・ロールバック手順（簡易）
- デプロイ前にマイグレーションの確認を行う
- ロールアウト: ブルー/グリーンまたはローリングデプロイを推奨
- ロールバック: マイグレーションが不可逆でないことを確認。必要ならダミーテーブルで互換性を維持

## 13. チェックリスト（レビュー前）
- [ ] リクエスト/レスポンスのスキーマがドキュメント化されている
- [ ] 入力バリデーションがサーバー側で網羅されている
- [ ] パスワードがハッシュ化されて保存されている
- [ ] 重複チェックが実装され、競合時のハンドリングがある
- [ ] 単体/統合テストが追加されている
- [ ] ログ出力とエラーハンドリングが適切に行われている
- [ ] レート制限やCAPTCHA等のボット対策が検討されている

## 14. 参考
- Spring Security ドキュメント
- BCryptPasswordEncoder
- Flyway / Liquibase

---

作業担当者はこの手順に従い実装し、変更点が発生したら本ドキュメントを更新すること。
