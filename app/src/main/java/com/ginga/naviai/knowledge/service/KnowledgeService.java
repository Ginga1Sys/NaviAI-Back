package com.ginga.naviai.knowledge.service;

import com.ginga.naviai.knowledge.dto.KnowledgePageResponse;
import com.ginga.naviai.knowledge.dto.KnowledgeSearchRequest;

/**
 * 記事検索サービスのインターフェース。
 */
public interface KnowledgeService {

    /**
     * リクエストパラメータに基づいて記事を検索し、ページング結果を返す。
     *
     * @param request 検索条件（q, sort, filter, page, size, tags）
     * @return ページングされた記事一覧
     */
    KnowledgePageResponse search(KnowledgeSearchRequest request);
}
