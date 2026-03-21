package com.ginga.naviai.knowledge.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * 記事検索結果のページングレスポンスDTO。
 * GET /api/v1/knowledge のルートレスポンス。
 */
@Data
@Builder
public class KnowledgePageResponse {

    /** 現在のページ番号（0始まり）*/
    private int page;

    /** 1ページあたりの取得件数 */
    private int size;

    /** 条件に合致する総件数 */
    private long totalElements;

    /** 記事一覧 */
    private List<KnowledgeItemDto> items;
}
