package com.ginga.naviai.knowledge.dto;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.List;

/**
 * 記事検索結果の1件を表すDTO。
 * GET /api/v1/knowledge のレスポンス内 items 配列の要素。
 */
@Data
@Builder
public class KnowledgeItemDto {

    /** 記事ID */
    private Long id;

    /** 記事タイトル */
    private String title;

    /** 記事の要約（本文の先頭200文字）*/
    private String summary;

    /** 投稿者のユーザー名 */
    private String author;

    /** 作成日時 */
    private Instant createdAt;

    /** スコア（いいね数）*/
    private long score;

    /** タグ一覧 */
    private List<String> tags;
}
