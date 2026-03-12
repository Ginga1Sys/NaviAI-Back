# タグ一覧取得API 作業手順

## 1. 概要
- 目的: 検索結果一覧画面で表示するタグ一覧を取得するREST API (`GET /api/v1/tags`) を実装する。
- 想定利用元: フロントエンドの検索結果一覧画面（タグフィルタ、タグ雲表示等）。

## 2. 前提条件
- 開発対象リポジトリ: `NaviAI-Back`
- 既存の参考実装: `GET /api/v1/dashboard`, `GET /api/v1/knowledge` の実装を参考にすること。
- 実行環境: Docker を利用したテスト実行 (`docker compose -f docker-compose.test.yml up --build`)。

## 3. 参考資料
- 要件定義書: `proj-1sys-ax-2025/docs/00_personal/nishizawa/統合要件定義書.md`
- 基本設計書: `proj-1sys-ax-2025/docs/00_personal/nishizawa/基本設計書/基本設計_API.md`
- 画面イメージ: `proj-1sys-ax-2025/docs/00_personal/nishizawa/基本設計書/画面イメージ図/SCR-04_検索結果_イメージ図.svg`
- 実装方針の参考: 既存のサマリ／検索APIの実装パターン

## 4. 成果物（想定ファイル・クラス）
- パッケージ: `com.ginga.naviai.tags`
- エンティティ: `Tag` (`Tag.java`)
- リポジトリ: `TagRepository` (`TagRepository.java`) - JPA
- サービス: `TagService`, `TagServiceImpl` (`TagService.java`, `TagServiceImpl.java`)
- コントローラ: `TagController` (`TagController.java`)
- DTO: `TagResponse` (`TagResponse.java`)（必要に応じて）
- 単体テスト: `TagControllerTest`, `TagServiceTest`
- ドキュメント: 本ファイル `タグ一覧取得API_作業手順.md`、および `NaviAI-Back/docs/nishizawa/NaviAI-Back_ディレクトリ構成.md` の更新

## 5. API 仕様（例）
- エンドポイント: `GET /api/v1/tags`
- 機能: システム内で使用されているタグの一覧を返却する。必要に応じて使用頻度（count）や表示用のメタ情報を含める。

- リクエスト: なし（認証が必要な場合は適宜 `Authorization` ヘッダを利用）

- レスポンス例 (200 OK):
```
[
  {"name": "機械学習", "count": 42},
  {"name": "Next.js", "count": 18},
  {"name": "設計", "count": 12}
]
```

- ステータスコード:
  - `200` 成功（タグ配列）
  - `500` サーバーエラー

## 6. 実装方針
- 既存の `knowledge` 系API と同じアーキテクチャ（Controller -> Service -> Repository）で実装する。
- DB のタグ情報が未整備の場合は新規テーブル `tags` を作成し、記事とタグの関連を管理する中間テーブル（例: `knowledge_tags`）を用意する。既存テーブルにタグ列があればそれを活用する。
- パフォーマンスを考慮し、タグの集計（count）はDB側で集計する（SQL の GROUP BY を使用）。
- キャッシュを後段で導入する可能性があるため、Service 層で返却型を固定しておく。

## 7. 詳細な実装手順
1. パッケージ作成
   - `NaviAI-Back/app/src/main/java/com/ginga/naviai/tags` を作成。

2. エンティティ / リポジトリ
   - `Tag` エンティティを作成（`id`, `name` など）。
   - 既存の `knowledge` エンティティとタグ関連がある場合は、それを参照する設計にする。
   - `TagRepository` を `JpaRepository<Tag, Long>` で実装し、タグ一覧取得用のクエリ（count付き）を用意する。例: `@Query("SELECT t.name AS name, COUNT(k.id) AS count FROM Knowledge k JOIN k.tags t GROUP BY t.name")`。

3. サービス実装
   - `TagService` インターフェースに `List<TagResponse> getAllTags()` を定義。
   - `TagServiceImpl` で `TagRepository` を利用し、DBから集計済みのタグリストを取得して `TagResponse` にマッピングする。

4. コントローラ実装
   - `TagController` に `@GetMapping("/api/v1/tags")` を追加し、`TagService#getAllTags()` を返却する。

5. 例外処理
   - 既存の `GlobalExceptionHandler` を活用して、例外発生時に適切な JSON を返すようにする。

6. 単体テスト作成（全実装完了後）
   - `TagServiceTest`:
     - DBアクセスをモックし、期待される `TagResponse` が返ることを検証する。
   - `TagControllerTest`:
     - MockMvc 等を使用して `GET /api/v1/tags` が `200` を返し、レスポンスボディが期待の JSON 構造であることを検証する。
   - テストコメントには「何を確認しているか」を明記する（例: "タグ一覧がcount付きで返ることを確認する"）。

7. ドキュメント更新
   - 実装が完了したら `NaviAI-Back/docs/nishizawa/NaviAI-Back_ディレクトリ構成.md` を更新し、追加したクラス・ファイルを追記する。

## 8. テスト手順
1. ローカルでのビルド（Docker を利用）
```
cd NaviAI-Back
docker compose -f docker-compose.test.yml up --build
```
2. コンテナ内でテストを実行（上記コマンドに含まれている場合は不要）
3. テスト項目:
   - すでに実装されている単体テストと合わせて全テストが成功すること。
   - `TagServiceTest` と `TagControllerTest` が期待どおりのアサーションを満たすこと。

## 9. チェックリスト（実装前確認・実装後確認）
- [ ] 要件定義・基本設計の参照先を確認した
- [ ] パッケージ `com.ginga.naviai.tags` を作成した
- [ ] `Tag` エンティティ / `TagRepository` を実装した
- [ ] `TagService` / `TagServiceImpl` を実装した
- [ ] `TagController` を実装した（`GET /api/v1/tags` を追加）
- [ ] 単体テスト（サービス・コントローラ）を実装した
- [ ] Docker 上でテストを実行し、成功を確認した
- [ ] `NaviAI-Back_ディレクトリ構成.md` を更新した
- [ ] 実装内容のレビュー依頼を作成した（レビュアー宛に伝える）

## 10. 補足・運用メモ
- レスポンスに `count` を含めるかどうかはフロント要件に依存する。表示のみなら `name` のみ返却しても良い。
- タグが多い場合はページネーションやクライアント側の表示制御を検討する。

---
作業手順書を作成しました。実装を進める前にレビューが必要であれば指示ください。
