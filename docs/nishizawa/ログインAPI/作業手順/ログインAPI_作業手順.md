# ログインAPI 作業手順

更新履歴
- 2026-01-29: 初版作成

目的
- 本手順はバックエンドのログインAPI（`POST /api/v1/auth/login`）を実装・テスト・レビューするための手順を示します。要件定義書および基本設計書に準拠して、安全に認証を行い、クライアントへ必要な情報を返却します。

前提条件
- 開発ブランチがチェックアウトされていること（例: `feature/registration-user-api` をベースに作業する場合はその旨を確認）。
- ローカルでアプリをビルド・テストできる環境（JDK、Maven）が整っていること。
- `NaviAI-Back/app/src/main/java/com/ginga/naviai/` 配下の既存の認証関連エンティティ（`User`、`ConfirmationToken` 等）が存在すること。
- パスワードはデータベースにハッシュ（BCrypt）で保存されていること。

ゴール（完了条件）
- `POST /api/v1/auth/login` で正しい資格情報を送ると 200 を返し、ユーザー情報（`UserResponse` 形式）とトークン（JWT またはプロジェクト既存方式）を返す。
- 無効な資格情報は 401、無効な入力は 400、アカウント未有効化は 403 を返す。
- 単体テストを追加すること。

API 仕様（基本）
- エンドポイント: `POST /api/v1/auth/login`
- Content-Type: `application/json`
- リクエストボディ:

```json
{
  "usernameOrEmail": "example@ginga.info",
  "password": "P@ssw0rd"
}
```

- 成功時レスポンス（例: 200）:

```json
{
  "user": {
    "id": 1,
    "username": "taro",
    "email": "example@ginga.info",
    "displayName": "太郎",
    "createdAt": "2026-01-01T12:00:00Z"
  },
  "token": "<jwt-or-session-token>",
  "expiresIn": 3600
}
```

- エラーケース:
  - 400 Bad Request: リクエストバリデーションエラー（必須項目欠落など）
  - 401 Unauthorized: 認証失敗（ユーザー名/メールまたはパスワード不正）
  - 403 Forbidden: アカウント未有効化（メール確認待ち等）
  - 500 Internal Server Error: 想定外のサーバーエラー

実装手順（エンジニア向け）
1. コントローラの追加/更新
   - ファイル: `NaviAI-Back/app/src/main/java/com/ginga/naviai/auth/controller/AuthController.java`
   - `@PostMapping("/api/v1/auth/login")` ハンドラを追加する。リクエストDTO（`LoginRequest`）を受け取り、`AuthService` を呼び出す。

2. DTO の定義
   - `LoginRequest` を `auth/dto` パッケージに追加。
     - フィールド: `usernameOrEmail`（`@NotBlank`）、`password`（`@NotBlank`）。
   - 既存の `UserResponse` を使用して成功時にクライアントへ返却する。

3. サービス層の実装
   - `AuthService` に `login(LoginRequest request)` を追加（インターフェース定義）。
   - `AuthServiceImpl` で次を行う:
     - `UserRepository` を使って `usernameOrEmail` で `User` を検索（`findByUsername` または `findByEmail`）。
     - 見つからない場合は認証失敗（401）。
     - `BCryptPasswordEncoder#matches(raw, encoded)` でパスワード検証。
     - アカウントが `enabled == false` の場合は 403 を返す。
     - 認証成功時にトークンを発行（プロジェクト既存のトークン発行ユーティリティを利用）。
     - `UserResponse` とトークンを組み合わせて返却。

4. 例外と HTTP ステータスのマッピング
   - `GlobalExceptionHandler` に `AuthenticationException` 相当を 401 にマップ。
   - `AccountNotEnabledException` を作成して 403 にマップ。

5. セキュリティ考慮
   - パスワードは平文でログ出力しない。
   - レートリミット（ブルートフォース対策）を検討し、必要なら API 層かゲートウェイで適用。
   - トークンは短めの有効期限とし、リフレッシュトークン戦略が必要なら設計する。

テスト手順
1. 単体テスト（必須）
   - `AuthServiceImpl` のユニットテストを `src/test/java/...` に追加する。
   - モック `UserRepository`、`PasswordEncoder` を使い、少なくとも次のケースを検証する: 成功、パスワード不一致、未有効ユーザー。
   - 新規テストを追加する際は、既存の単体テスト群が影響を受けないことを必ず確認する（下記実行手順参照）。

2. コントローラの単体/スライステスト（推奨）
   - `@WebMvcTest` を使って `AuthController` の入力検証とレスポンス形式を検証する。実装が複雑な場合は `@SpringBootTest` + モック構成で代替可。

3. 統合／e2e テスト（保留）
   - 他 API が一通り揃うまで統合テスト・e2e は保留。統合実施時は別途手順に従う。

ローカルでのテスト実行と既存テストの検証（必須）
- まず全ての単体テストを実行し、既存テストが通っていることを確認してください。

```powershell
# プロジェクトルートから単体テストのみ実行（統合テストをスキップ）
mvn -f app/pom.xml -DskipITs=true test

# 全てのテスト（統合テストを含む）を実行する場合
mvn -f app/pom.xml test
```

- 特定のテストクラスだけを実行してデバッグする場合:

```powershell
# 例: AuthServiceImpl のユニットテストのみ実行
mvn -f app/pom.xml -Dtest=com.ginga.naviai.auth.service.AuthServiceImplTest test
```

- 期待される動作:
  - 既存のテストがすべて成功すること（失敗がある場合は先に修正）。
  - 新規テストは成功すること。
  - もし既存テストが新規実装により失敗する場合は、そのテストを壊した変更を最小限に絞って修正してください。

トラブルシュート
- 依存する Bean の差分でテストが落ちる場合、テスト用にモックを追加して影響範囲を隔離する。
- データベース周りで失敗する場合は H2 のテスト設定やテストデータ初期化（`data.sql` / `@Sql`）を見直す。

テスト完了後の手順
- 全単体テストが成功したら、PR にテスト結果と簡単な説明を付けてレビュー依頼を出す。


ローカルでの実行コマンド（例）
```powershell
# プロジェクトルートで（NaviAI-Back/app）
mvn -f app/pom.xml test -DskipITs=false

# アプリ起動（開発用）
mvn -f app/pom.xml spring-boot:run
```

デバッグ/検証手順
- DB にテストユーザーを用意（パスワードは BCrypt でハッシュ化）。H2 コンソールや SQL マイグレーションを使って作成。
- Postman / curl でログインを試行し、期待するステータスと JSON を確認する。

curl 例:

```bash
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"usernameOrEmail":"taro@example.com","password":"P@ssw0rd"}'
```

Dockerでのテスト実行（推奨）
- 既にプロジェクトに用意されている `Dockerfile.test` と `docker-compose.test.yml` を使って、コンテナ内でユニットテストを実行できます。これによりローカル環境差分の影響を減らせます。
- 実行場所: リポジトリの `NaviAI-Back` ディレクトリ（`docker-compose.test.yml` が存在する場所）でコマンドを実行してください。

```powershell
# フルテスト実行（サービスをビルドしてユニットテストを実行、実行後に終了）
docker compose -f docker-compose.test.yml up --build --abort-on-container-exit --exit-code-from backend-unit-tests backend-unit-tests

# 古い環境で docker-compose を使う場合
docker-compose -f docker-compose.test.yml up --build --abort-on-container-exit --exit-code-from backend-unit-tests backend-unit-tests
```

- 特定のテストクラスだけ実行したい場合は `run` で `mvn` コマンドをオーバーライドできます:

```powershell
# 例: AuthServiceImpl のユニットテストのみ実行
docker compose -f docker-compose.test.yml run --rm backend-unit-tests mvn -f pom.xml -Dtest=com.ginga.naviai.auth.service.AuthServiceImplTest test
```

- 注意:
  - `backend-unit-tests` サービスは `backend-app` に依存しています（`depends_on`）。`depends_on` は起動順序を保証しますが、アプリの準備完了まで待機するわけではありません。必要であればヘルスチェックを追加してください。
  - `Dockerfile.test` は依存関係を事前にダウンロードするためにビルド時に `mvn -DskipTests package` を実行します。これによりテストランが高速化されます。
  - ボリュームマウント（`./app:/workspace:cached`）により、ホストのソース更新がコンテナに反映されます。


デプロイ/運用上の注意
- ログに機微な情報（パスワード、フルトークン）を出力しないこと。
- トークン失効・ブラックリスト戦略がある場合、該当テーブルやキャッシュを設計する。
- ログイン試行の監査ログ（日時、IP、成功/失敗）を保持する場合は保存場所と保持期間を定義する。

レビュー基準チェックリスト
- `LoginRequest` にバリデーション注釈がある。
- パスワード比較に `BCryptPasswordEncoder` を使用している。
- 失敗ケースで適切な HTTP ステータスを返している（401/403/400）。
- ユニット・統合テストが追加され、`mvn test` で成功する。
- セキュリティ上の不要なログ出力がない。
- API ドキュメント（README または OpenAPI）を更新した。

次の作業（提案）
- 実装完了後、`NaviAI-Back/docs/nishizawa/ログインAPI/作業手順/ログインAPI_作業手順.md` をレビューに回してください。
- レビュー承認後、CI を通してマージします。

参照
- 要件定義書・基本設計書（プロジェクト内 `docs/`）
