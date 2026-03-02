**Project Overview**
- **Name:** NaviAI-Back — backend for NaviAI project.
- **Language / Framework:** Java, Spring Boot.

**Prerequisites**
- **Docker / Docker Compose:** 推奨。ローカルでコンテナ実行する手順を以下に示します。
- **JDK 17+**: ローカルで直接実行する場合に必要。
- **Gradle Wrapper:** プロジェクト内に `gradlew` を同梱しています。

**Quick Start (Docker Compose)**
- このリポジトリはテスト用の Compose ファイルを含みます: [docker-compose.test.yml](docker-compose.test.yml)
- アプリは内部で H2 を使うため、追加の DB 構築は不要です。Redis を使うので Docker Compose に含めています。

1. リポジトリをクローン

```bash
git clone <repository-url>
cd NaviAI-Back
```

2. Docker Compose で起動 (テスト / 開発向け)

```bash
docker compose -f docker-compose.test.yml up --build
# バックグラウンド実行する場合
docker compose -f docker-compose.test.yml up --build -d

# ログ確認
docker compose -f docker-compose.test.yml logs -f backend-app
```

- Compose は Redis とアプリ（`backend-app`）を起動します。アプリは `./app` をボリュームマウントしているため、ソース修正が即座に反映されます。
- アプリはデフォルトでポート `8080` を公開します。

**ローカルで直接実行 (開発時)**
- ソースディレクトリに移動して実行します。

```bash
cd app
./gradlew --no-daemon bootRun
```

- またはビルドして Jar を実行:

```bash
cd app
./gradlew clean build
java -jar build/libs/*-SNAPSHOT.jar
```

**テスト実行**
- ユニット／統合テストは Gradle で実行します。

```bash
cd app
./gradlew test
```

または Compose の `backend-unit-tests` サービスを使う方法:

```bash
docker compose -f docker-compose.test.yml run --rm backend-unit-tests
```

**Docker イメージを手動でビルドして実行する**
- ビルド (例):

```bash
docker build -f app/Dockerfile.test -t ginga1sys/naviai-back:local ./app
```

- 実行 (環境変数を `.env` で管理することを推奨):

```bash
docker run --rm -p 8080:8080 \
  --env-file .env \
  ginga1sys/naviai-back:local
```

**環境変数と秘密情報**
- `application.properties` はデフォルトで H2 メモリ DB を使用します（開発用）。
- Redis の接続情報は環境変数 `REDIS_HOST`, `REDIS_PORT`, `REDIS_PASSWORD` を利用します。
- 本番用シークレット（`token.secret` など）は決してリポジトリにコミットしないでください。`.env` やシークレットマネージャに保管してください。

**マイグレーション**
- マイグレーションは `src/main/resources/db/migration` にある Flyway スクリプトで管理されています。アプリ起動時に自動適用されます。

**開発ルール / PR のガイドライン**
- 新規 PR には以下を含めてください: **目的**, **変更点**, **動作確認手順**, **影響範囲**。
- ブランチ命名例: `feature/xxx`, `fix/xxx`, `chore/xxx`。
- 小さな差分で PR を作成してレビューを容易にしてください。

**ビルド / テストコマンドまとめ**
- ローカル起動: `cd app && ./gradlew --no-daemon bootRun`
- 全ビルド: `cd app && ./gradlew clean build`
- ユニットテスト: `cd app && ./gradlew test`
- Docker Compose 起動: `docker compose -f docker-compose.test.yml up --build`

**お問い合わせ / 連絡先**
- 不明点はリポジトリの担当チームまたは Slack チャンネルへお問い合わせください（チーム内の連絡先を明記してください）。

---
変更履歴: 2026-03-02 - README を新規作成（目的: 初見の開発者が迷わないための起動手順を追加）
