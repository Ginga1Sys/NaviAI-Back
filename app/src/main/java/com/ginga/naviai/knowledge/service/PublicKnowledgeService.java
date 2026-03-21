package com.ginga.naviai.knowledge.service;

import com.ginga.naviai.knowledge.dto.PublicRecommendedKnowledgeResponse;
import com.ginga.naviai.knowledge.dto.PublicKnowledgePageResponse;
import org.springframework.data.domain.Pageable;

/**
 * 公開トップ画面（SCR-12）向けナレッジ取得サービス。
 * 認証不要エンドポイントからのみ利用する。
 */
public interface PublicKnowledgeService {

    /**
     * 公開記事一覧を取得する。
     * visibility=public かつ status=published かつ未削除の記事のみ返す。
     */
    PublicKnowledgePageResponse getPublicKnowledge(Pageable pageable);

    /**
     * 「今週の注目」向けのおすすめ記事を取得する。
     * 非公開記事（visibility=private）をいいね数降順で返す。
     */
    PublicRecommendedKnowledgeResponse getRecommendedKnowledge(int limit);
}
