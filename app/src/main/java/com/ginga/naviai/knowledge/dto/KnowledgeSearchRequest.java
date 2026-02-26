package com.ginga.naviai.knowledge.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

/**
 * 記事検索リクエストパラメータを保持するDTO。
 * GET /api/v1/knowledge のクエリパラメータにバインドされる。
 */
@Data
public class KnowledgeSearchRequest {

    /** 検索語（タイトル・本文の部分一致）。省略可。 */
    private String q;

    /**
     * ソート指定。省略可。
     * 有効値: created_at（投稿日時降順）| -created_at（同上）| score（いいね数降順）
     */
    private String sort;

    /**
     * ビジネスロジックによる絞り込み。省略可。
     * 有効値: recommended（いいね数降順のおすすめ記事）| latest（新着、最大20件）
     */
    private String filter;

    /** ページ番号（0始まり）。省略時はデフォルト0。 */
    @Min(value = 0, message = "pageは0以上を指定してください")
    private int page = 0;

    /** 1ページあたりの件数。省略時はデフォルト20。 */
    @Min(value = 1, message = "sizeは1以上を指定してください")
    @Max(value = 100, message = "sizeは100以下を指定してください")
    private int size = 20;

    /** タグによる絞り込み（カンマ区切り）。省略可。 */
    private String tags;
}
