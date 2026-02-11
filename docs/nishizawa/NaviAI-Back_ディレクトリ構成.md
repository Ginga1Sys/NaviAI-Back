# NaviAI-Back/app/src/main ディレクトリ構成

以下は `NaviAI-Back/app/src/main` 配下の主要ソースと、それぞれの処理内容の簡単な説明です。

- app/src/main
  - java
    - com
      - ginga
        - naviai
          - NaviAiBackApplication.java: Spring Boot アプリケーションのエントリポイント（main メソッドで起動）。
          - config
            - AsyncRetryConfig.java: 非同期実行とリトライを有効化する設定。`mailTaskExecutor`（ThreadPoolTaskExecutor）を定義。
            - SecurityConfig.java: パスワードハッシュ用の `BCryptPasswordEncoder` Bean を提供（強度 10）。
          - mail
            - MailService.java: メール送信の抽象インターフェース（`send` メソッド）。
            - SmtpMailService.java: `MailService` 実装。`JavaMailSender` を使って `SimpleMailMessage` を送信する。`@Async` と `@Retryable` を使用し、送信失敗時のリカバリ処理とテスト用の `simulateFailure` フラグを備える。
          - auth
            - controller
              - AuthController.java: 認証関連の REST API。`/register` でユーザー登録を受け付け、`/login` で認証（ユーザー名/メール + パスワード）を受け付けてトークンを返す。`/confirm` でメール確認トークンを検証してユーザーを有効化する処理を持つ。
            - dto
              - UserResponse.java: クライアントへ返すユーザー情報の DTO（id, username, email, displayName, createdAt）。
              - RegisterRequest.java: 登録リクエストの DTO。バリデーション注釈（`@NotBlank`, `@Email`, `@Size`, ドメイン制限 `@Pattern`, カスタム `@StrongPassword`）を含む。
              - LoginRequest.java: ログインリクエストの DTO（`usernameOrEmail`, `password`）。
              - LoginResponse.java: ログイン成功時に返す DTO（`UserResponse`、`token`、`expiresIn`）。
            - entity
              - User.java: `users` テーブルに対応する JPA エンティティ。ユーザー名、メール、パスワードハッシュ、表示名、有効フラグ、作成/更新日時などを保持。
              - ConfirmationToken.java: メール確認用トークンを表すエンティティ。ユーザー参照、作成時刻、有効期限、確認済み時刻を管理。
            - exception
              - DuplicateResourceException.java: ユーザー名やメールアドレスが重複している場合に投げるランタイム例外。
              - InvalidCredentialsException.java: ログイン時の認証失敗（ユーザーまたはパスワード不正）を表す例外。
              - AccountNotEnabledException.java: ユーザーがメール確認などで有効化されていない場合に投げられる例外。
              - GlobalExceptionHandler.java: `@ControllerAdvice` による共通例外ハンドラー。バリデーションエラーを整形して 400 を返し、重複リソースは 409、認証失敗や未有効アカウントに対しては適切な HTTP ステータスを返す処理を含む。
            - repository
              - UserRepository.java: `User` 用の `JpaRepository`。`findByUsername` / `findByEmail` を提供。
              - ConfirmationTokenRepository.java: `ConfirmationToken` 用の `JpaRepository`。`findByToken` を提供。
            - service
              - AuthService.java: 認証サービスのインターフェース（`register`, `login`）。
              - AuthServiceImpl.java: 登録とログイン処理の実装。登録では重複チェック、ユーザー作成（初期は無効化）、トークン作成と確認メール送信を行う。ログインでは `usernameOrEmail` と `password` を検証し、パスワード不一致や未有効化の場合はそれぞれ例外を投げ、成功時は一時トークン（現在は UUID をプレースホルダ）と有効期限を含む `LoginResponse` を返す。
              - ConfirmationTokenService.java: トークン生成（UUID）、有効期限設定、トークン検索、確認時刻の更新を行うサービス。
            - validation
              - StrongPassword.java: パスワード強度チェック用のカスタム検証アノテーション。
              - StrongPasswordValidator.java: カスタムバリデータ。大文字・小文字・数字・記号のカテゴリ数を数え、3種以上かつ長さ >= 8 で有効と判定。
  - resources
    - application.properties: H2 インメモリ DB の接続設定、JPA 設定（ddl-auto=update）、ログレベル、H2 コンソール有効化などの環境設定。
    - db
      - migration
        - V1__create_users_table.sql: `users` テーブル作成用のマイグレーション SQL（id, username, email, password_hash, display_name, enabled, created_at, updated_at を定義）。


> 生成日時: 2026-01-27
