package com.ginga.naviai.knowledge.service;

import com.ginga.naviai.knowledge.dto.KnowledgeDetailResponse;
import com.ginga.naviai.knowledge.dto.KnowledgeResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import com.ginga.naviai.knowledge.dto.KnowledgePageResponse;
import com.ginga.naviai.knowledge.dto.KnowledgeSearchRequest;

import java.util.Optional;

public interface KnowledgeService {
    Page<KnowledgeResponse> getMyKnowledgeByUsername(String username, Pageable pageable);
    Page<KnowledgeResponse> getKnowledgeByAuthorId(Long authorId, Pageable pageable);

    /**
     * リクエストパラメータに基づいて記事を検索し、ページング結果を返す。
     *
     * @param request 検索条件（q, sort, filter, page, size, tags）
     * @return ページングされた記事一覧
     */
    KnowledgePageResponse search(KnowledgeSearchRequest request);

    /**
     * 記事詳細を取得する。
     *
     * @param id              記事ID
     * @param currentUsername 認証ユーザーのusername（いいね済み判定・編集可否判定に使用）
     * @return 記事詳細DTO。該当記事が存在しないまたは論理削除済みの場合はempty
     */
    Optional<KnowledgeDetailResponse> getKnowledgeDetail(Long id, String currentUsername);
}
