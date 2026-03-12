### 新規作成・修正が必要なファイル一覧

| ファイル名 | ディレクトリ | 役割 |
| :--- | :--- | :--- |
| `Post.java` | `app/src/main/java/com/ginga/naviai/post/entity/` | **投稿エンティティ**: 投稿の情報を表現するデータベースのテーブルに対応するクラスです。投稿ID、タイトル、内容、作成日、ユーザー情報などが含まれます。 |
| `PostRepository.java` | `app/src/main/java/com/ginga/naviai/post/repository/` | **投稿リポジトリ**: `Post`エンティティのデータベース操作（データの保存、取得、更新、削除）を行うインターフェースです。 |
| `PostResponse.java` | `app/src/main/java/com/ginga/naviai/post/dto/` | **投稿レスポンスDTO**: APIがクライアントに返す投稿の情報を格納するクラスです。エンティティから必要な情報のみを選んでクライアントに渡します。 |
| `PostService.java` | `app/src/main/java/com/ginga/naviai/post/service/` | **投稿サービス (インターフェース)**: 投稿に関するビジネスロジックを定義するインターフェースです。投稿の取得などのメソッドを宣言します。 |
| `PostServiceImpl.java` | `app/src/main/java/com/ginga/naviai/post/service/` | **投稿サービス (実装)**: `PostService`インターフェースを実装するクラスです。投稿を取得する具体的な処理を記述します。 |
| `PostController.java` | `app/src/main/java/com/ginga/naviai/post/controller/` | **投稿コントローラー**: HTTPリクエストを受け付けるエンドポイントです。`/api/posts` のようなURLでリクエストを受け取り、`PostService`を呼び出して結果をクライアントに返します。 |
| `V2__create_posts_table.sql` | `app/src/main/resources/db/migration/` | **DBマイグレーションファイル**: `posts`テーブルをデータベースに作成するためのSQLスクリプトです。Flywayなどのマイグレーションツールによって実行されます。 |
| `User.java` | `app/src/main/java/com/ginga/naviai/auth/entity/` | **ユーザーエンティティ (修正)**: 既存のユーザーエンティティに、ユーザーが持つ投稿のリストを関連付けるための修正を追加します。 |





# 投稿一覧（マイ投稿）API — 必要ファイル一覧

## 概要
「投稿一覧（マイ投稿）」API実装に必要な新規作成・修正ファイルと役割を簡潔にまとめます。

## ファイル一覧（優先順）
- Post.java  
  - ディレクトリ: app/src/main/java/com/ginga/naviai/post/entity/  
  - 役割: 投稿エンティティ（id, title, body, createdAt, author など）
- PostRepository.java  
  - ディレクトリ: app/src/main/java/com/ginga/naviai/post/repository/  
  - 役割: DBアクセス（投稿の検索・ページング等）
- PostResponse.java  
  - ディレクトリ: app/src/main/java/com/ginga/naviai/post/dto/  
  - 役割: クライアントへ返すレスポンスDTO
- PostService.java  
  - ディレクトリ: app/src/main/java/com/ginga/naviai/post/service/  
  - 役割: ビジネスロジック定義（例：ユーザーの投稿取得メソッド）
- PostServiceImpl.java  
  - ディレクトリ: app/src/main/java/com/ginga/naviai/post/service/  
  - 役割: Service実装（Repository呼び出し、DTO変換、ページング）
- PostController.java  
  - ディレクトリ: app/src/main/java/com/ginga/naviai/post/controller/  
  - 役割: HTTPエンドポイント（例：GET /api/my/posts）
- V2__create_posts_table.sql  
  - ディレクトリ: app/src/main/resources/db/migration/  
  - 役割: postsテーブル作成用マイグレーション（Flyway等）
- User.java（修正）  
  - ディレクトリ: app/src/main/java/com/ginga/naviai/auth/entity/  
  - 役割: 投稿との関連付け追加（例：OneToMany）

## 実装メモ（短）
- エンドポイント例: GET /api/my/posts?userId={id}&page={page}&size={size}  
- ページング・ソート・認可（ログインユーザー検証）を考慮すること
