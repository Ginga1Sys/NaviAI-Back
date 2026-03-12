package com.ginga.naviai.knowledge.controller;

import com.ginga.naviai.knowledge.dto.KnowledgePageResponse;
import com.ginga.naviai.knowledge.dto.KnowledgeSearchRequest;
import com.ginga.naviai.knowledge.service.KnowledgeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 記事検索結果一覧を提供するコントローラ。
 *
 * <p>エンドポイント: {@code GET /api/v1/knowledge}
 *
 * <p>認証は SecurityConfig の {@code anyRequest().authenticated()} ルールにより必須。
 * 期限切れ・不正トークンはJWTフィルタが 401 を返す。
 */
@RestController
@RequestMapping("/api/v1/knowledge")
@RequiredArgsConstructor
public class KnowledgeController {

    private final KnowledgeService knowledgeService;

    /**
     * クエリパラメータを受け取り、条件に合致する記事一覧をページング形式で返す。
     *
     * @param request 検索条件（q, sort, filter, page, size, tags）
     * @return 200: 記事一覧、400: パラメータ不正
     */
    @GetMapping
    public ResponseEntity<KnowledgePageResponse> search(
            @Valid @ModelAttribute KnowledgeSearchRequest request) {
        KnowledgePageResponse response = knowledgeService.search(request);
        return ResponseEntity.ok(response);
    }
}
