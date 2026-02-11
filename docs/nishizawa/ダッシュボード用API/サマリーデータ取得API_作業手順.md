# サマリーデータ取得API 作業手順

作成日: 2026-02-07

## 概要
- 目的: ダッシュボード表示用のサマリーデータを取得する `GET /api/v1/dashboard` エンドポイントを実装する。
- 対象アプリ: NaviAI-Back（Spring Boot, Maven）

## 前提条件
- ログイン・トークン更新・ユーザー情報取得APIが実装済みであること。
- 開発環境が整っていること（Java、Maven、Docker 等）。

## 参考資料
- 要件定義: proj-1sys-ax-2025\docs\00_personal\nishizawa\統合要件定義書.md
- 基本設計（API）: proj-1sys-ax-2025\docs\00_personal\nishizawa\基本設計書\基本設計_API.md
- 基本設計（DB）: proj-1sys-ax-2025\docs\00_personal\nishizawa\基本設計書\基本設計_DB設計.md
- 実装方針は `docs/nishizawa/ダッシュボード用API/サマリーデータ取得API_作業内容.md` を踏襲する。

## 成果物（想定ファイル/クラス）
- Controller: `DashboardController` (`/api/v1/dashboard` の REST エンドポイント)
- Service: `DashboardService` / `DashboardServiceImpl`
- DTO: `DashboardSummaryResponse`, 必要に応じて `SummaryItem` 等
- Repository: 必要に応じて新規 Repository（集計クエリ用）
- 単体テスト: `DashboardControllerTest`, `DashboardServiceTest`

## 実装方針
- Controller から Service を呼び、Service が Repository や既存のサービスを利用して集計データを組み立てる。
- 認可/認証は既存のセキュリティ設定（JWT フィルターなど）を流用する。
- DB 集計は可能な限り JPQL / Spring Data のカスタムクエリで実装し、複雑な処理は Service 層で整形する。
- 単体テストはモック（Mockito）で Controller と Service を分離して実施する。

## 詳細な実装手順
1. 仕様確定
   - 必要なサマリ項目を確定する（例: 総投稿数、今週の投稿数、未承認件数、人気タグ上位など）。
   - レスポンス形式（JSON）のスキーマを決める。

2. DTO 定義
   - `DashboardSummaryResponse` を `app/src/main/java/.../dto` 配下に追加。
   - 各フィールドに対する説明コメントを付ける（例: `totalPosts`, `weeklyPosts`, `pendingApprovals`）。

3. Repository / JPQL クエリ
   - 必要な集計クエリを `Repository` に追加する（ネイティブ SQL が必要なら `@Query(nativeQuery=true)` を検討）。
   - 大きな集計はパフォーマンスを考慮して必要に応じてインデックスや専用 SQL に切り替える。

4. Service 実装
   - `DashboardService` インターフェースを定義し、`getSummary()` 等のメソッドを追加。
   - `DashboardServiceImpl` で Repository を使って集計し、`DashboardSummaryResponse` を構築する。

5. マイグレーション（DB スキーマ）追加
    - 目的: サマリ集計で参照するコンテンツ系テーブル（`knowledge`, `tag`, `knowledge_tag`, `comment`, `like`, など）がデータベースに存在することを保証する。
    - 手順:
       1. 基本設計書（`proj-1sys-ax-2025/docs/00_personal/nishizawa/基本設計書/基本設計_DB.md`）のスキーマを参照し、実装で必要なテーブル一覧を確定する（最低: `knowledge`, `tag`, `knowledge_tag`, `comment`, `like`）。
       2. `NaviAI-Back/app/src/main/resources/db/migration/` 配下に Flyway 形式の SQL ファイルを追加する（例: `V3__create_knowledge_and_tag_tables.sql`）。
            - ファイル名規則: `V<version>__<description>.sql`（既存が V1/V2 のため次は V3 以降）。
            - 各 CREATE TABLE 文には `IF NOT EXISTS` を付け、必要なインデックスと外部キーを定義する。
       3. 追加するマイグレーションのサンプル内容（簡易例）:

```sql
CREATE TABLE IF NOT EXISTS knowledge (
   id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
   author_id UUID REFERENCES users(id),
   title VARCHAR(500) NOT NULL DEFAULT '',
   body TEXT,
   status VARCHAR(20) NOT NULL DEFAULT 'draft',
   is_deleted BOOLEAN NOT NULL DEFAULT false,
   created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
   updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
   published_at TIMESTAMPTZ
);

CREATE TABLE IF NOT EXISTS tag (
   id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
   name VARCHAR(100) NOT NULL UNIQUE,
   created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS knowledge_tag (
   knowledge_id UUID NOT NULL REFERENCES knowledge(id) ON DELETE CASCADE,
   tag_id UUID NOT NULL REFERENCES tag(id) ON DELETE CASCADE,
   PRIMARY KEY (knowledge_id, tag_id)
);

CREATE TABLE IF NOT EXISTS comment (
   id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
   knowledge_id UUID NOT NULL REFERENCES knowledge(id) ON DELETE CASCADE,
   author_id UUID REFERENCES users(id),
   body TEXT NOT NULL,
   parent_comment_id UUID REFERENCES comment(id) ON DELETE SET NULL,
   is_deleted BOOLEAN NOT NULL DEFAULT false,
   created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS "like" (
   id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
   knowledge_id UUID NOT NULL REFERENCES knowledge(id) ON DELETE CASCADE,
   user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
   created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
   UNIQUE (knowledge_id, user_id)
);

CREATE INDEX IF NOT EXISTS idx_knowledge_author_created ON knowledge (author_id, created_at);
CREATE INDEX IF NOT EXISTS idx_knowledge_status_published ON knowledge (status, published_at);
CREATE INDEX IF NOT EXISTS idx_tag_name ON tag (name);
```

       4. マイグレーション適用手順（ローカル）:

```bash
cd NaviAI-Back/app
mvn -DskipTests=true package
# アプリ起動時に Flyway が自動適用される設定ならアプリを起動
java -jar target/*.jar
```

       5. CI または Docker テスト環境での適用:
            - `docker-compose.test.yml` の DB コンテナを起動するときに Flyway が実行されるか確認する（必要なら `flyway` イメージや `entrypoint` で SQL を適用）。
       6. 適用確認: `psql` / DB クライアントでテーブルが存在することを確認し、簡易クエリでデータが取得できるか検証する。

    - 注意点:
       - 既存の本番 DB がある場合はマイグレーション前にバックアップを必ず取得する。
       - 既に同名テーブルが存在する環境ではスキーマ差分を確認して手動マイグレーションが必要になる場合がある。

6. Controller 実装
   - `DashboardController` を `@RestController` として追加し、`@GetMapping("/api/v1/dashboard")` を実装。
   - 認証済みユーザー向けのエンドポイントであれば `@PreAuthorize` 等を利用する。

7. 単体テスト実装
   - `DashboardServiceTest`: Repository をモックして集計ロジックを検証する。
   - `DashboardControllerTest`: MockMvc を使ってエンドポイントのステータスとレスポンス構造を検証する。
   - 既存テストと同様に `@SpringBootTest` / `@WebMvcTest` を使い分ける。

8. ローカル実行・確認
   - 単体テストを実行して全てパスすることを確認する（下記参照のコマンド）。

9. ドキュメント更新
   - 実装が完了したら `NaviAI-Back/docs/nishizawa/NaviAI-Back_ディレクトリ構成.md` 等の一覧に成果物を追記する。

## 単体テスト実行手順（ローカル/Maven）
1. Maven でプロジェクトルートからテストを実行する（NaviAI-Back/app 配下で実行する場合の例）:

```bash
cd NaviAI-Back/app
mvn test
```

2. Docker コンテナ内で実行する（既存の `docker-compose.test.yml` を利用する手順例）:

```bash
cd NaviAI-Back
docker-compose -f docker-compose.test.yml up --build --abort-on-container-exit
docker-compose -f docker-compose.test.yml down
```

※ コンテナ内での test 実行やログ取得方法は既存の `README` や CI 定義に合わせて調整する。

## チェックリスト
- [ ] 仕様（出力項目・型）が決定されている
- [ ] DTO が実装されている
- [ ] 必要な Repository / クエリが実装されている
- [ ] Service ロジックが実装されている
- [ ] Controller が実装されエンドポイントが動作する
- [ ] 単体テスト（Controller/Service）が作成されパスする
- [ ] ドキュメント（ディレクトリ構成等）を更新した

## 付録: API 仕様（サンプル）
- エンドポイント: `GET /api/v1/dashboard`
- リクエスト例:

```bash
curl -X GET "http://localhost:8080/api/v1/dashboard" \
   -H "Authorization: Bearer <ACCESS_TOKEN>" \
   -H "Accept: application/json"
```

- レスポンス例:

```json
{
   "totalPosts": 1240,
   "weeklyPosts": 18,
   "pendingApprovals": 3,
   "topTags": [
      {"tag":"AI","count":230},
      {"tag":"NLProc","count":87}
   ]
}
```

---
作成者: 自動生成（必要に応じて担当者名・日付を追記してください）
