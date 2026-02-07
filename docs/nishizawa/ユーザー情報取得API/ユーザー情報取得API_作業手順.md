# ユーザー情報取得API 作業手順書

## 概要
- エンドポイント: `GET /api/v1/users/me`
- 認証済みユーザーの情報を返す。クライアントはログイン済み（有効なアクセストークン所持）を前提とする。

## 前提条件
- ログインAPI、トークン更新API 等の認証基盤が実装済みであること（JWT や Security 設定など）。
- 参照：既存の `AuthController`, `AuthService`, `User`, `UserRepository`, `UserResponse` 等の実装。

## 目的
- 現在認証されているユーザーの基本情報（id, username, email, displayName, createdAt 等）を取得して返却する。

## 期待する挙動
- 成功: HTTP 200 と `UserResponse` を返却。
- 未認証: HTTP 401 を返却。
- ユーザー不存在（理論上起きないが）: HTTP 404 を返却。

## 依存実装 / 参照ファイル
- 既存の認証関連実装（参照のみの想定だが、必要に応じて改修可能）:
   - `app/src/main/java/com/ginga/naviai/auth/filter/JwtAuthenticationFilter.java`
   - `app/src/main/java/com/ginga/naviai/auth/util/TokenUtil.java`
   - `app/src/main/java/com/ginga/naviai/config/SecurityConfig.java`
   - 既存のエンティティ/リポジトリ/DTO（流用可能であれば再利用）:
      - `app/src/main/java/com/ginga/naviai/auth/entity/User.java`
      - `app/src/main/java/com/ginga/naviai/auth/repository/UserRepository.java`
      - `app/src/main/java/com/ginga/naviai/auth/dto/UserResponse.java`
   - 注意: `AuthController` / `AuthService` は認証専用のため、基本的に改修せずに新しいパッケージを作成する方針を取る。

（実プロジェクトのパスは上記を参考に調整してください）

## 実装手順（推奨順）
1. 既存コード確認
   - `AuthController` と `AuthService`、`UserResponse` の構造を確認し、返却 DTO のフィールドを確定する。

2. API 設計確定
   - エンドポイント: `GET /api/v1/users/me`
   - レスポンスボディ: `UserResponse`（id, username, email, displayName, enabled, createdAt 等）

3. コントローラ作成（推奨: 新規パッケージ）
    - `AuthController` / `AuthService` は認証専用のため、既存ファイルを直接変更せずに新規で `user` パッケージを作成する。
       - 例: `app/src/main/java/com/ginga/naviai/user/controller/UserController.java`
    - エンドポイント例: `@GetMapping("/api/v1/users/me") public ResponseEntity<UserResponse> getCurrentUser()`
    - 認証情報の取得方法（いずれか）:
       - `@AuthenticationPrincipal` を使用して `UserDetails` を受け取る。
       - `SecurityContextHolder.getContext().getAuthentication()` から principal（ユーザーID）を取り、`UserService` で DB から取得する。
    - コントローラはサービス（`UserService`）を注入して DTO を返す責務のみにする。

4. サービス層実装（新規パッケージ）
   - `app/src/main/java/com/ginga/naviai/user/service/UserService.java` を作成し、`getCurrentUser()` を実装する。
   - 処理: Authentication（principal）→ `UserRepository` でユーザーを取得 → `UserResponse` に変換して返却。
   - 可能であれば既存の `UserRepository`/`User` エンティティを再利用し、ロジックの重複を避ける。

5. DTO / マッピング
   - 既存の `app/src/main/java/com/ginga/naviai/auth/dto/UserResponse.java` がフロント要件と一致する場合は再利用する。
   - 要件が違う場合は `app/src/main/java/com/ginga/naviai/user/dto/UserResponse.java` を新規作成して、エンティティ → DTO のマッピングを行う。
   - マッピングは手動で問題ないが、MapStruct 等の導入が既にあるなら利用を検討する。

6. セキュリティ確認
   - `SecurityConfig` において `/api/v1/users/me` が認証必須になっていることを確認する。
   - `JwtAuthenticationFilter` が `Authentication` にユーザー識別子（JWT の `sub` を userId として扱う）を確実にセットしていることを確認する。
   - `@AuthenticationPrincipal` を使う場合は、`UserDetails` 実装が principal として利用可能か確認する。

7. 単体テスト実装
   - コントローラの単体テスト（`MockMvc` + モックサービス）を作成する。
   - ケース: 正常（200 + body）、未認証（401）、ユーザー未存在（404）を検証する。
   - テスト例ファイル: `app/src/test/java/com/ginga/naviai/user/controller/UserControllerTest.java`

8. 統合テスト（任意）
   - 認証フローと合わせてエンドツーエンドで動作するか確認するテストを追加する（テストコンテナ等）。

9. ビルド・テスト実行
   - Docker コンテナ内で実行（既存の `docker-compose.test.yml` を利用する場合）:
     ```powershell
     cd NaviAI-Back
     docker-compose -f docker-compose.test.yml up --build --abort-on-container-exit
     ```
   - テスト完了後、コンテナを停止してクリーンアップする:
     ```powershell
     docker-compose -f docker-compose.test.yml down --volumes --remove-orphans
     ```

10. レビュー & マージ
    - 実装後はコードレビューを依頼する。特にセキュリティと DTO の公開情報に注意する。

## サンプル要求／レスポンス
- リクエスト例:
  - GET /api/v1/users/me
  - Authorization: Bearer <access_token>
- 正常レスポンス例 (HTTP 200):
```json
{
  "id": 1,
  "username": "yamada",
  "email": "yamada@example.com",
  "displayName": "山田 太郎",
  "createdAt": "2025-01-01T12:34:56Z"
}
```

## 注意事項
- 返却する情報は最小限に留め、パスワードやセキュリティ関連の情報は絶対に含めない。
- API 仕様がフロントエンド側（`NaviAI-Front`）の期待と整合しているか事前確認する。

## 参考・補足
- 追加で実装するユースケースがある場合（例：管理者が別ユーザーを取得）：別エンドポイントに分離する。
- 実装後にフロント側の e2e テスト（`NaviAI-Front/e2e`）を更新し、UI 側で表示が正しいか確認すると安全。

---
作成日: 2026-02-06
作成者: 自動生成（手順書テンプレート）
