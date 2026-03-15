package com.ginga.naviai.knowledge.controller;

import com.ginga.naviai.knowledge.dto.PublicKnowledgePageResponse;
import com.ginga.naviai.knowledge.dto.PublicRecommendedKnowledgeResponse;
import com.ginga.naviai.knowledge.service.PublicKnowledgeService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 公開トップ画面（SCR-12）向けナレッジ取得コントローラー。
 * 認証不要（SecurityConfig で /api/v1/public/** を permitAll に設定済み）。
 */
@RestController
@RequestMapping("/api/v1/public/knowledge")
@RequiredArgsConstructor
public class PublicKnowledgeController {

    private final PublicKnowledgeService publicKnowledgeService;

    /**
     * 公開記事一覧を取得する。
     *
     * @param page ページ番号（0始まり、デフォルト: 0）
     * @param size 1ページあたりの件数（デフォルト: 10）
     * @return 公開記事のページングレスポンス
     */
    @GetMapping
    public ResponseEntity<PublicKnowledgePageResponse> getPublicKnowledge(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Pageable pageable = PageRequest.of(page, Math.min(size, 50));
        return ResponseEntity.ok(publicKnowledgeService.getPublicKnowledge(pageable));
    }

    /**
     * 今週の注目（おすすめ）記事を取得する。
        * 非公開記事（visibility=private）をいいね数の多い順で返す。
     *
     * @param limit 取得件数（デフォルト: 1、最大: 10）
     */
    @GetMapping("/recommended")
    public ResponseEntity<PublicRecommendedKnowledgeResponse> getRecommendedKnowledge(
            @RequestParam(defaultValue = "1") int limit) {
        return ResponseEntity.ok(publicKnowledgeService.getRecommendedKnowledge(limit));
    }
}
