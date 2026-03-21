# 記事検索結果一覧取得API 作業手順書

## 概要
- 目的: ダッシュボードや検索画面から呼び出す記事検索結果一覧を提供するREST APIを実装する。
- エンドポイント: `GET /api/v1/knowledge`

## 前提条件
- 既存のサマリAPI（`GET /api/v1/dashboard`）を参照して実装すること。
- 開発環境はリポジトリの `NaviAI-Back` を使用する。
- 実装は `NaviAI-Back/app/src/main/java/com/ginga/naviai/knowledge` 配下に配置する。

## 参考資料
- 要件定義: proj-1sys-ax-2025/docs/00_personal/nishizawa/統合要件定義書.md
- 基本設計: proj-1sys-ax-2025/docs/00_personal/nishizawa/基本設計書/基本設計_API.md
- ダッシュボード画面イメージ: proj-1sys-ax-2025/docs/00_personal/nishizawa/基本設計書/画面イメージ図/

## 想定成果物（ファイル/クラス名例）
- コントローラ: `com.ginga.naviai.knowledge.controller.KnowledgeController`
- サービス: `com.ginga.naviai.knowledge.service.KnowledgeService` / `KnowledgeServiceImpl`
- DTO: `com.ginga.naviai.knowledge.dto.KnowledgeResponse`、`KnowledgeSummaryDto`、`KnowledgeSearchRequest` 等
- リポジトリ/エンティティ（必要なら）: `com.ginga.naviai.knowledge.entity.Knowledge`、`KnowledgeRepository`
- 単体テスト: `KnowledgeControllerTest`、`KnowledgeServiceTest`

## API仕様（例）
- エンドポイント
  - `GET /api/v1/knowledge`

- リクエストパラメータ（クエリ）
  - `q` (optional): 検索語（全文検索、タイトル/本文/タグ）
   - `sort` (optional): ソート指定。`created_at`（投稿順）|`score`（スコア順）|`-created_at`（降順指定など）
   - `filter` (optional): 意味的/ビジネスロジックによる絞り込み。以下の値を想定。
             - `recommended`: いいね数が多い記事をおすすめと定義する。実装では期間・重み付けを考慮した上で `likes` の降順（いいね数の多い順）でスコア化して返す。
                - 注意: 本手順書では `recommended` の個人化要素は未実装の前提だが、ユーザー固有の推薦を行う場合は `Authorization: Bearer <token>` を付与して呼び出すことを想定する（認証必須）。
      - `latest`: 新着（作成日時降順）、最大20件取得
      - カスタムタグフィルタ（例: `filter=team:infra`）は将来的に拡張可能
  - `page` (optional): ページ番号（0始まり、デフォルト0）
     (注：フロントエンドの画面でページが切り替えるたびにこのパラメータを更新して呼び出す想定)
  - `size` (optional): 1ページあたり件数（デフォルト20）
  - `tags` (optional): タグによるフィルタ（カンマ区切り）

- レスポンス例 (HTTP 200)
{
  "page": 0,
  "size": 20,
  "totalElements": 123,
  "items": [
    {
      "id": 1,
      "title": "記事タイトル",
      "summary": "記事の要約テキスト...",
      "author": "username",
      "createdAt": "2026-02-20T12:34:56Z",
      "score": 4.5,
      "tags": ["AI","設計"]
    }
  ]
}

- HTTPステータス
  - 200: 正常
  - 400: リクエストパラメータ不正
  - 500: サーバエラー

- `filter` と `sort` の利用ルール
   - `filter` は意味的/ビジネスロジックによる絞り込みに使い、`sort` は最終的な並び順指定に使う。
   - 例: `?filter=recommended&sort=created_at&page=0&size=5` → 推薦フィルタで絞った結果を作成日時で降順ソートして返す。
   - `filter=recommended` はユーザーコンテキストに依存するため、個人化推奨時は `Authorization: Bearer <token>` を必須とすることを推奨。

## 実装方針
- 既存のサマリAPI実装を参考にレイヤー構成（Controller→Service→Repository）で実装する。
- 検索ロジックは可能ならDBの全文検索機能（H2またはRDBの全文検索）やJPQL/Criteriaを利用する。
 - `filter=recommended`（ダッシュボードの「おすすめ」）は本手順書の方針として「いいね数が多い記事」を意味し、サーバ側で `likes` の降順で抽出・返却する実装を想定する（必要に応じて期間フィルタや重み付けを追加）。将来的に推薦エンジンを導入する場合はこの挙動を拡張する。
- 入力パラメータはバリデーションを行い、不正な値は400を返す。
- 単体テストはControllerとService層を中心に作成する。DB依存はRepositoryをモックするか、インメモリDBでインテグレーション風に確認する。

## 詳細な実装手順
1. ディレクトリ作成
   - `NaviAI-Back/app/src/main/java/com/ginga/naviai/knowledge/controller`
   - `.../service`
   - `.../dto`
   - `.../entity`（必要なら）

2. DTO定義
   - `KnowledgeSearchRequest`（`q`, `sort`, `page`, `size`, `tags`）
   - `KnowledgeResponse`（上記レスポンスのアイテム）
   - ページング用レスポンスラッパー（`page`, `size`, `totalElements`, `items`）

3. エンティティ／リポジトリ（必要なら）
   - 既存のテーブル定義で記事データが存在するか確認。存在しない場合はマイグレーションSQLを作成して追加する（`id, title, body, summary, author_id, created_at, tags, score` 等）。

4. Service実装
   - `KnowledgeService#search(KnowledgeSearchRequest)` を実装。Repository呼び出しで検索・ソート・ページングを行い、DTOへ変換して返却する。

5. Controller実装
   - `@GetMapping("/api/v1/knowledge")` を作成し、クエリパラメータを受け取ってServiceを呼ぶ。
   - リクエストバリデーションと例外ハンドリング（400）を実装。
   - コントローラ/メソッドレベルに `@PreAuthorize("isAuthenticated()")` または `@PreAuthorize("hasRole('USER')")` を付与する。
   - あるいは `SecurityConfig` 側で該当パスに対して認証を必須に設定する（例: `http.authorizeRequests().antMatchers("/api/v1/knowledge").authenticated()` など）。
   - 認証は既存のJWTフィルタ/ミドルウェアで行い、期限切れや不正トークンはミドルウェアで `401` を返す前提とする。

6. 単体テスト作成
   - `KnowledgeServiceTest`: 検索ロジック（クエリとソートの分岐）を検証する。Repositoryをモックして期待値を確認する。コメントで「何を検証しているか」を明記する。
   - `KnowledgeControllerTest`: エンドポイントのパラメータ受け取りとステータス/レスポンス形状を検証する。MockMvcを利用する。
     - 認証検証: `filter=recommended` を個人化モードで使う場合、認証がない・期限切れトークンの場合に `401 Unauthorized` が返ることを確認するテストを追加する（MockMvc で Security の設定をモックして検証）。

7. ビルド & テスト（ローカル）
   - `NaviAI-Back` ルートで `mvn -q -DskipTests=false test` を実行して単体テストが通ることを確認する（ただし最終はDockerで実行）。

8. Dockerでテスト実行 (CI相当)
   - プロジェクトルート（`NaviAI-Back` フォルダ）で以下を実行：

```bash
# docker-compose.test.yml を使ってビルドしてテストを実行する
docker compose -f docker-compose.test.yml up --build
```

9. 結果確認とドキュメント更新
   - テストが通ったら、`NaviAI-Back/docs/nishizawa/NaviAI-Back_ディレクトリ構成.md` を更新して実装ファイルの一覧を追加する。

## 単体テストで確認すべき項目（例）
- サービス層
  - 検索語 `q` がある場合、Repositoryに正しい検索条件が渡されること。
  - `sort=created_at` の際は `createdAt DESC` でソートされる結果を返すこと。
  - `filter=latest` の際は `createdAt DESC` でソートされる結果を20件返すこと。
  - `filter=recommended` の際は `likes` の降順（いいね数の多い順）で返すこと（おすすめ定義の検証）。
  - ページング（`page`, `size`）が正しく適用されること。
- コントローラ層
  - 必須でないパラメータが未指定でも200を返すこと。
  - 不正なパラメータ（page=-1 など）で400を返すこと。
  - レスポンススキーマが仕様と一致していること。

## チェックリスト
- [ ] `KnowledgeController` を追加した
- [ ] `KnowledgeService` を実装した
- [ ] 必要なDTOを追加した
- [ ] DBテーブル/マイグレーションを作成した（必要な場合）
- [ ] 単体テストを実装してコメントで検証内容を明記した
- [ ] `docker compose -f docker-compose.test.yml up --build` でテストが通った
- [ ] `NaviAI-Back/docs/nishizawa/NaviAI-Back_ディレクトリ構成.md` を更新した

## 出力先・ファイル名
- 本手順書ファイル: NaviAI-Back/docs/nishizawa/記事検索結果一覧取得API/記事検索結果一覧取得API_作業手順.md

---
作業中に要件やDB構成の不明点が出たら都度確認してください。