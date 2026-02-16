# ログアウトAPI 作業手順書

## 概要
本作業手順書は、AIナレッジ共有サイトのバックエンドにおけるログアウトAPI（POST /api/v1/auth/logout）を実装・テストする手順を記載する。

## 前提条件
- 既にログインAPI、ユーザー登録API、トークン更新APIが実装されていること。
- 既存の認証関連（トークン発行/保存/検証）実装を再利用する。
- 単体テストは Docker コンテナ内で実行して動作確認する。

## エンドポイント仕様（要件）
- HTTP メソッド：POST
- パス：/api/v1/auth/logout
- 説明：クライアントのログアウトを行い、サーバ側で管理するリフレッシュトークンやセッション情報があれば無効化する。
- 想定認証：ログイン済み（アクセス時にアクセストークンを検証する）

## リクエスト例
- Authorization ヘッダに Bearer アクセストークンを含める。
- Body は基本的に不要だが、実装次第で `refreshToken` を受け取る設計にしても良い。

例（アクセストークンのみ）
```
POST /api/v1/auth/logout
Authorization: Bearer <access_token>
```

例（リフレッシュトークンを渡す場合）
```
POST /api/v1/auth/logout
Content-Type: application/json
{
  "refreshToken": "<refresh_token>"
}
```

## レスポンス例
- 成功 200 OK
```
{ "message": "Logged out successfully" }
```
- 認証失敗 401 Unauthorized
- リソース不整合 400/409（必要に応じて）

## 実装方針（サーバ側）
1. ルーティング
   - `AuthController`（既存）に `@PostMapping("/logout")` を追加。
   - エンドポイントは `/api/v1/auth/logout` に合わせる。

2. 認可・認証チェック
   - アクセストークンで利用者を特定する既存ロジックを再利用する。
   - 必要に応じて `@PreAuthorize` 等の注釈を付与。

3. トークン無効化
   - サーバ側でリフレッシュトークンを DB（`RefreshToken` テーブル等）で管理している場合は、該当トークンを削除または無効化する。
   - クライアント送信の `refreshToken` を受け取る設計なら、受け取って DB 上で該当行を削除する。
   - トークンをブラックリストで管理している場合はブラックリストへ追加する処理を実装する。

4. セッション情報の破棄
   - もしセッション（キャッシュ、Redis 等）を使っている場合は関連セッションを削除する。

5. サービス層
   - `AuthService` に `logout(String username, Optional<String> refreshToken)` 的なメソッドを追加し、コントローラから呼ぶ。

6. 例外ハンドリング
   - 既存の `GlobalExceptionHandler` を利用し、適切なステータスとメッセージを返す。

7. アクセストークンの `jti` 付与と Redis ブラックリスト（推奨）
    - 目的: アクセストークン（短寿命 JWT）の即時無効化を実現するため、発行時に `jti` を付与し、ログアウト時にその `jti` を Redis にブラックリスト登録して検証する。
    - 概要フロー:
       1. アクセストークン発行時に UUID 形式の `jti` を JWT クレームに含める。
       2. リフレッシュトークン管理（`refresh_tokens` テーブル）にも `jti` を格納して、アクセストークンとリフレッシュトークンを紐づける（既に `jti` カラムがあれば流用）。
       3. `/api/v1/auth/logout` 実行時に `refresh_tokens` レコードを `revoked=true`（および `revoked_at=now()`）に更新し、アクセストークンの `jti` を Redis に TTL 付きで登録する（TTL = アクセストークンの残存秒数）。
       4. 認可フィルタ（JWT 検証後）で Redis を照会し、`jti` がブラックリストに存在する場合はリクエストを拒否（401）。

    - 実装メモ（サーバ側・Java 例）:
       - アクセストークン発行（TokenProvider 等）
          ```java
          String jti = UUID.randomUUID().toString();
          claims.put("jti", jti);
          String token = createJwt(claims, expiresIn);
          // リフレッシュトークンと紐づける場合は refresh_tokens.jti に保存
          ```

       - ログアウト処理（AuthService.logout 内）
          ```java
          // 1) リフレッシュトークンを取り消す
          refreshToken.setRevoked(true);
          refreshToken.setRevokedAt(Instant.now());
          refreshTokenRepository.save(refreshToken);

          // 2) アクセストークンの jti を Redis に登録（TTL = tokenExpirySeconds）
          String blacklistKey = "auth:blacklist:" + accessTokenJti;
          redisTemplate.opsForValue().set(blacklistKey, "true", tokenExpirySeconds, TimeUnit.SECONDS);
          ```

       - 認可フィルタ（JWT 検証後）
          ```java
          String jti = claims.get("jti", String.class);
          String blacklistKey = "auth:blacklist:" + jti;
          if (Boolean.TRUE.equals(redisTemplate.hasKey(blacklistKey))) {
                throw new JwtAuthenticationException("Token revoked");
          }
          ```

    - テスト追加（単体・統合）:
       - `AuthServiceTest` で `logout()` が `refreshToken.revoked=true` をセットすることを検証。
       - 認可フィルタの単体テストで、Redis にブラックリストがある場合に 401 を返すケースを追加（`RedisTemplate` をモック）。
       - 追加 E2E/統合テスト: ログアウト後にアクセストークンで保護APIへアクセスすると 401 になることを検証。

    - 運用上の注意:
       - Redis の TTL がアクセストークンの残存時間と一致するようにすること。
       - ブラックリストはインメモリ管理が望ましく、DB にブラックリストを置くと負荷が高くなる点に注意。

## 実装手順（具体）
1. `AuthController` にメソッド追加
   - シグネチャ例：
     ```java
     @PostMapping("/logout")
     public ResponseEntity<?> logout(@RequestBody(required = false) LogoutRequest req, @AuthenticationPrincipal UserPrincipal user) {
         authService.logout(user.getUsername(), Optional.ofNullable(req != null ? req.getRefreshToken() : null));
         return ResponseEntity.ok(Map.of("message","Logged out successfully"));
     }
     ```
   - `LogoutRequest` DTO（`refreshToken` フィールド）を必要に応じて追加。

2. `AuthService` に処理を追加
   - DB に保存されたリフレッシュトークンを削除、あるいは無効化するロジックを実装する。

3. `ConfirmationToken` 等とは別に `RefreshTokenRepository`（既にあれば流用）を利用する。

4. 単体テスト（JUnit）を追加
   - `AuthControllerTest` と `AuthServiceTest` にログアウトの正常系・異常系テストを追加。
   - モック（`@MockBean`）で `AuthService` や `RefreshTokenRepository` をモックして挙動確認を行う。

5. `jti` / Redis ブラックリスト に関する具体実装手順（追加）
    - DB マイグレーション
       - `refresh_tokens` テーブルに `jti` カラム（VARCHAR(255) / UUID）を追加するマイグレーションを作成。既に存在する場合はスキップ。

    - トークン発行側（TokenProvider 等）の変更
       - アクセストークン発行時に `jti` を生成して JWT のクレームに含める。
       - リフレッシュトークン発行時に同じ `jti` を `refresh_tokens.jti` に保存して紐付ける。
       - 例（疑似コード）:
          ```java
          String jti = UUID.randomUUID().toString();
          Claims claims = new Claims();
          claims.put("sub", username);
          claims.put("jti", jti);
          String accessToken = tokenProvider.createToken(claims, accessTokenExpiry);
          // refresh token 作成時に refreshToken.setJti(jti);
          ```

    - `AuthController.logout` の呼び出し設計
       - Authorization ヘッダ（または `SecurityContext`）からアクセストークンを検証し、クレームの `jti` を取得する。
       - 必要に応じてリクエストボディで `refreshToken` を受け取る。
       - コントローラは `authService.logout(username, accessTokenJti, Optional.ofNullable(refreshToken))` を呼ぶ。

    - サービス層の実装（`AuthService.logout`）
       - 受け取った `refreshToken` があれば `refresh_tokens` テーブルの該当レコードを `revoked=true`/`revoked_at=now()` に更新する。
       - アクセストークンの `jti` を Redis にキー `auth:blacklist:<jti>` で登録し、TTL をアクセストークンの残存秒数に設定する。
       - DB 更新と Redis 登録は可能ならトランザクションで整合性確保（Redis 操作はトランザクション外の場合が多いので注意）。

    - Redis 設定・Bean
       - `spring-data-redis` 依存を `pom.xml` に追加。
       - `RedisTemplate<String,String>`（または `StringRedisTemplate`）を Bean 定義し、`application.properties` に接続情報を追加する。

    - 認可フィルタ（`JwtAuthenticationFilter` 等）の変更
       - JWT を検証した後、クレームから `jti` を取得して Redis の `auth:blacklist:<jti>` を照会する。
       - 存在する場合は認証失敗として 401 を返す（`JwtAuthenticationException` 等をスローして `GlobalExceptionHandler` で処理）。

    - DTO / ユーティリティ
       - 必要なら `LogoutRequest` DTO（`refreshToken` フィールド）を追加。
       - アクセストークンの残存秒数を算出するユーティリティ（`TokenProvider.getRemainingSeconds(token)`）を実装。

    - マイグレーション / CI 変更
       - DB マイグレーションファイル（Flyway/Liquibase 等）を追加し、CI パイプラインで実行されるように設定。
       - `pom.xml` に Redis 依存を追加し、CI イメージに Redis クライアントが利用可能か確認。

    - テスト計画の追加
       - `AuthServiceTest`: `logout()` が該当 `refreshToken` を `revoked=true` に更新することを検証（`RefreshTokenRepository` をモック）。
       - 認可フィルタ単体テスト: Redis モックで `jti` がブラックリストにある場合にリクエストが拒否されることを検証。

    - 運用メモ
       - Redis の TTL はアクセストークンの残存時間と一致させる。短寿命トークンを採用することでブラックリストの負荷を抑えられる。
       - 既存のトークン管理方針（個別取り消し vs 全体無効化）をドキュメントに明記する。

## 単体テスト実行（Docker 環境）
- ローカルで Docker を使ってテストを回す手順例：

```powershell
# NaviAI-Back ルートで（ワークスペース内パスに移動）
cd NaviAI-Back
# コンテナでテスト用サービスをビルドして実行（プロジェクトに合わせてファイル名を調整）
docker compose -f docker-compose.test.yml up --build --abort-on-container-exit
# テスト終了後、コンテナを落とす
docker compose -f docker-compose.test.yml down
```

- 直接 Maven で実行する場合（ローカル環境で Maven が使える場合）：

```powershell
cd NaviAI-Back/app
mvn test
```

## テスト項目（例）
- 正常系：認証済みユーザーがログアウトリクエストを送ると 200 が返り、DB のリフレッシュトークンが削除される。
- 異常系：無効なトークン、未ログイン状態でアクセスした場合に 401 を返すこと。
- DB にトークンが存在しない場合の挙動（安全に成功扱いにする、または 404 を返す等の方針を合わせる）。

## 既存資産の再利用箇所（確認ポイント）
- `AuthController`, `AuthService`, `RefreshTokenRepository`（または既存のトークン管理クラス）
- `GlobalExceptionHandler`（例外ハンドリング）
- 単体テストの共通設定（`@SpringBootTest` テストコンフィグやモック設定）

## 注意事項・設計上の決めごと（要合意）
- ログアウト時にアクセストークン自体をサーバ側で無効化する設計はコストが高い。一般的にはリフレッシュトークンの削除とクライアント側でのトークン破棄で対応する。
- レスポンスはセキュリティ上、過度な情報を返さない（成功/失敗の最小限の情報に留める）。
- 既存 API との互換性を崩さないこと。

## レビューとリリース手順
1. コード実装後、該当テストを追加し、`mvn test`（または Docker 上のテスト）で全テストが通ることを確認。
2. プルリク作成時に変更点（コントローラ、サービス、リポジトリ、テスト）を明記し、レビューを依頼する。
3. レビュー完了後、CI を走らせ、問題なければ main ブランチへマージする。

## 参考資料
- 既存のログイン/トークン更新実装（リポジトリ内の `AuthController`, `AuthService`, `RefreshToken` 管理実装を参照）
- ドキュメント: `NaviAI-Back/docs/nishizawa/ログインAPI`（既存 API 実装手順）

---
作業担当者は実装後にレビュー申請を行ってください。必要であれば本手順書を更新して追記してください。
