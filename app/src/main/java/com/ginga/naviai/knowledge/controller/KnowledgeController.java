package com.ginga.naviai.knowledge.controller;

import com.ginga.naviai.knowledge.dto.KnowledgeDetailResponse;
import com.ginga.naviai.knowledge.dto.KnowledgeResponse;
import com.ginga.naviai.knowledge.dto.KnowledgePageResponse;
import com.ginga.naviai.knowledge.dto.KnowledgeSearchRequest;
import com.ginga.naviai.knowledge.service.KnowledgeService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;


import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@RestController
@RequestMapping("/api/v1/knowledge")
public class KnowledgeController {

    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of("createdAt", "title", "publishedAt");

    private final KnowledgeService knowledgeService;

    public KnowledgeController(KnowledgeService knowledgeService) {
        this.knowledgeService = knowledgeService;
    }

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

    /**
     * 記事詳細取得 GET /api/v1/knowledge/{id}
     *
     * @param id          記事ID
     * @param userDetails 認証ユーザー情報（いいね済み判定・編集可否判定に使用）
     * @return 記事詳細レスポンス。存在しない場合は404
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> getKnowledgeDetail(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {

        String username = userDetails != null ? userDetails.getUsername() : null;
        Optional<KnowledgeDetailResponse> detail = knowledgeService.getKnowledgeDetail(id, username);

        if (detail.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Map<String, Object> response = new HashMap<>();
        response.put("data", detail.get());
        return ResponseEntity.ok(response);
    }
}
