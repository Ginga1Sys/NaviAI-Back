<<<<<<< HEAD
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
=======
package com.ginga.naviai.knowledge.service;

import com.ginga.naviai.knowledge.dto.KnowledgeResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface KnowledgeService {
    Page<KnowledgeResponse> getMyKnowledgeByUsername(String username, Pageable pageable);
    Page<KnowledgeResponse> getKnowledgeByAuthorId(Long authorId, Pageable pageable);
}
>>>>>>> 4d0036690621c93b680f4a1863ad2e8a4f3d0c24
