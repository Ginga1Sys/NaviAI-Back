# 週次アクティビティ集計データ取得API 作業手順

## 概要
- 目的: ダッシュボード用に週次（7日間）アクティビティ集計データ（投稿数、コメント数、いいね数の日別内訳）を提供するAPIを実装する。
- 対象エンドポイント: `GET /api/v1/dashboard/activity?range=week`

## 前提条件
- 既存のサマリデータ取得API（`GET /api/v1/dashboard`）が実装済みであること。
- 開発環境: `NaviAI-Back` フォルダ内でビルド・テストを実行できること（Docker を利用）。
- 必要な参照資料は本手順書の「参考資料」参照。

## 参考資料
- 要件定義: proj-1sys-ax-2025/docs/00_personal/nishizawa/統合要件定義書.md
- 基本設計: proj-1sys-ax-2025/docs/00_personal/nishizawa/基本設計書/基本設計_API.md
- 既存実装参照: `GET /api/v1/dashboard` のコントローラ／サービス実装（プロジェクト内の `summary` パッケージ）

## 成果物（想定ファイル・クラス）
- コントローラ: `SummaryController.java` に `GET /api/v1/dashboard/activity` エンドポイントを追加
- サービス: `SummaryService.java` / `SummaryServiceImpl.java` に週次集計メソッドを追加
- DTO: `ActivityResponse.java`, `ActivityDayItem.java`（日別集計）
- リポジトリ／クエリ: 必要に応じて `PostRepository` 等に集計用メソッドを追加（JPQL/native SQL）
- 単体テスト: `SummaryServiceTest.java`, `SummaryControllerTest.java`

-## API 仕様
- エンドポイント: `GET /api/v1/dashboard/activity`
- クエリパラメータ:
   - `from` (必須): 集計開始日（形式: `yyyy-MM-dd`）
   - `to` (必須): 集計終了日（形式: `yyyy-MM-dd`）
   - `range` (省略可): 集計レンジの意味合い。未指定時は `week` を想定する（将来的に `month` 等を想定）
- レスポンス (HTTP 200):

```json
{
  "range": "week",
  "from": "2026-02-08",
  "to": "2026-02-14",
  "items": [
    { "date": "2026-02-08", "posts": 3, "comments": 5, "likes": 10 },
    { "date": "2026-02-09", "posts": 1, "comments": 2, "likes": 4 }
  ]
}
```

## 実装方針
- 既存のサマリ系APIと同じ `summary` モジュール内で実装し再利用性を保つ。
- DB 集計は可能な限り単一クエリで取得（パフォーマンス優先）。DB 側で日付ごとの集計を行い、欠損日は 0 を補完する処理をサービス層で行う。
- 日付操作には `java.time` を使用する。
- 単体テストはコントローラ層とサービス層に分け、サービスはリポジトリをモックして集計ロジックを検証する。

## 詳細な実装手順
1. 既存の `summary` パッケージを確認する（コントローラ、サービス、DTOの位置を把握）。
2. DTO を追加
   - `ActivityDayItem`：`date (LocalDate)`, `posts (int)`, `comments (int)`, `likes (int)`
   - `ActivityResponse`：`range`, `from`, `to`, `items: List<ActivityDayItem>`
3. サービスインターフェースにメソッド追加
   - `ActivityResponse getActivity(LocalDate from, LocalDate to, String range)`
4. サービス実装での処理フロー
   - `from` と `to` は必須パラメータとして受け取り、指定された期間で集計を行う。
   - `range` が指定されない場合は意味合いとして `week` を採用する（ただし期間は `from`/`to` で決定される）。将来 `range` により `from`/`to` の自動補完を行う仕様に拡張できる。
   - リポジトリに日付ごとの投稿数・コメント数・いいね数を返すクエリを実装（例: GROUP BY DATE(created_at)）
   - 取得結果を日付範囲にマップし、欠損日は 0 で補完する。
   - `ActivityResponse` を生成して返す。
5. コントローラ実装
   - `@GetMapping("/activity")` を追加、`@RequestParam("from") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from`, `@RequestParam("to") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to`, `@RequestParam(name = "range", required = false) String range` を受け取る。
   - `range` が `null` の場合はサーバー側で `"week"` をデフォルト値に設定してサービスに渡す。
   - サービスの `getActivity(from, to, range)` を呼ぶ。
6. リポジトリ／クエリ実装の注意点
   - DB は H2（テスト）や本番 DB に依存するため、日付関数に注意する（テストでは H2 に合わせる）。
   - パフォーマンスを考慮し、可能なら単一クエリで 3 種のカウントを取得し、サービス層でマージする。
7. 単体テスト実装（実装完了後にまとめて作成）
   - `SummaryServiceTest`:
     - モックリポジトリが返す集計結果を用意し、サービスが正しく日付補完・マージして `ActivityResponse` を生成することを検証する。
   - `SummaryControllerTest`:
     - MockMvc 等で `GET /api/v1/dashboard/activity?range=week` が 200 を返し、レスポンスの形式が想定どおりであることを確認する。
   - テストには各テストで「何を確認しているか」をコメントで明記する（テストファイル内コメント）。

## 単体テスト実行手順（Docker）
1. `NaviAI-Back` フォルダに移動する。

```powershell
cd "d:\AI駆動開発デモ\NaviAI-Back"
```

2. テスト用コンテナをビルドしてテストを実行する（既存手順に合わせる）。

```powershell
docker compose -f docker-compose.test.yml up --build
```

3. テスト結果を確認する。失敗があればログを確認し、該当クラスの実装／テストを修正する。

## チェックリスト
- [ ] `Activity` DTO を追加した
- [ ] サービスに `getActivity` を実装した
- [ ] 必要なリポジトリ／クエリを実装した
- [ ] コントローラにエンドポイントを追加した
- [ ] サービス／コントローラの単体テストを実装した（各テストに検証目的コメントあり）
- [ ] `docker compose -f docker-compose.test.yml up --build` でテスト実行・確認した
- [ ] `NaviAI-Back/docs/nishizawa/NaviAI-Back_ディレクトリ構成.md` を更新するための差分メモを作成した

## 補足（実装上の注意）
- 日別の欠損データ補完はサービス側で必ず行うこと（フロントが簡単に扱えるフォーマットにする）。
- 将来的に `range` に `month` 等が追加されることを想定し、`range` のパースロジックは拡張可能な設計にする。

---
作成者: （担当者名）
作成日: 2026-02-15
