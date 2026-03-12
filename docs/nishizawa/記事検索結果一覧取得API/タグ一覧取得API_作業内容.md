# タグ一覧取得API

## 前提条件
- AIナレッジ共有サイトのWebアプリで使用するバックエンドのAPI開発を進行中
- サマリーデータ取得API（`GET /api/v1/dashboard`）などはすでに実装済み
- 要件定義書（`proj-1sys-ax-2025\docs\00_personal\nishizawa\統合要件定義書.md`）と基本設計書（`proj-1sys-ax-2025\docs\00_personal\nishizawa\基本設計書\基本設計_API.md`、`proj-1sys-ax-2025\docs\00_personal\nishizawa\基本設計書\基本設計_DB設計.md`）の内容を考慮して、作業手順の検討と実装を行う。
- 検索結果一覧のイメージ図: `proj-1sys-ax-2025\docs\00_personal\nishizawa\基本設計書\画面イメージ図\SCR-04_検索結果_イメージ図.svg`
- ディレクトリ構成: `NaviAI-Back\docs\nishizawa\NaviAI-Back_ディレクトリ構成.md`

## 実装作業の内容
- GET /api/v1/tags エンドポイントの実装
- サマリーデータ取得API（`GET /api/v1/dashboard`）、記事検索結果一覧取得API（`GET /api/v1/knowledge`）はすでに実装済みのため、参考にして実装を進めること。
- `NaviAI-Back\app\src\main\java\com\ginga\naviai\tags`を新規に作成し、その配下で実装を行う。
- 検索結果一覧の画面に表示するタグの一覧を取得するためのAPIを実装する。
- DBのテーブル定義が新規に必要な場合はそちらも追加する。
- テストは単体テストまで実施する。
- すべてのPGの実装が完了してから単体テストの実装を開始する。
- 単体テストには何を確認しているかコメント文で明記する。
- 単体テストの動作確認はdockerコンテナ内で実行する。
- 単体テストは実装済みの単体テストの動作確認も一緒に実行する。
- 「NaviAI-Back」フォルダ配下で「docker compose -f docker-compose.test.yml up --build」を実行する。
- dockerコンテナの立ち上げ、テストの実行まで行う。
- 実装とテストの実行まですべて完了したら、ディレクトリ構成のファイルを更新する。
（NaviAI-Back\docs\nishizawa\NaviAI-Back_ディレクトリ構成.md）

## 手順書に記載する内容
- 概要
- 前提条件
- 参考資料、参考にする要件定義書と基本設計書のファイルパス
- 成果物（想定ファイルやクラス名など）
- APIの仕様（エンドポイント、リクエスト、レスポンス例）
- 実装方針
- 詳細な実装手順
- チェックリスト

## 手順書の出力先とファイル名
- 出力先: 「NaviAI-Back\docs\nishizawa\記事検索結果一覧取得API」配下
- ファイル名: 「タグ一覧取得API_作業手順.md」