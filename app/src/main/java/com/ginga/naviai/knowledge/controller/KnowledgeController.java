<<<<<<< HEAD
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
=======
package com.ginga.naviai.knowledge.controller;

import com.ginga.naviai.knowledge.dto.KnowledgeResponse;
import com.ginga.naviai.knowledge.service.KnowledgeService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/api/v1/knowledge")
public class KnowledgeController {

    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of("createdAt", "title", "publishedAt");

    private final KnowledgeService knowledgeService;

    public KnowledgeController(KnowledgeService knowledgeService) {
        this.knowledgeService = knowledgeService;
    }

    @GetMapping
    public ResponseEntity<?> getKnowledge(
            @RequestParam(required = false) Boolean mine,
            @RequestParam(name = "author_id", required = false) Long authorId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(name = "per_page", defaultValue = "20") int perPage,
            @RequestParam(defaultValue = "createdAt") String sort,
            @AuthenticationPrincipal UserDetails userDetails) {

        if (!ALLOWED_SORT_FIELDS.contains(sort)) {
            return ResponseEntity.badRequest().body("Invalid sort field. Allowed: " + ALLOWED_SORT_FIELDS);
        }

        if (page < 1) {
            return ResponseEntity.badRequest().body("page must be >= 1");
        }

        Pageable pageable = PageRequest.of(page - 1, perPage, Sort.by(sort).descending());

        Page<KnowledgeResponse> knowledgePage;

        if (Boolean.TRUE.equals(mine)) {
            if (userDetails == null) {
                return ResponseEntity.status(401).body("Unauthorized");
            }
            knowledgePage = knowledgeService.getMyKnowledgeByUsername(userDetails.getUsername(), pageable);
        } else if (authorId != null) {
            if (userDetails == null) {
                return ResponseEntity.status(401).body("Unauthorized");
            }
            knowledgePage = knowledgeService.getKnowledgeByAuthorId(authorId, pageable);
        } else {
            return ResponseEntity.badRequest().body("Either 'mine' or 'author_id' must be provided.");
        }

        Map<String, Object> meta = new HashMap<>();
        meta.put("page", knowledgePage.getNumber() + 1);
        meta.put("per_page", knowledgePage.getSize());
        meta.put("total", knowledgePage.getTotalElements());

        Map<String, Object> response = new HashMap<>();
        response.put("data", knowledgePage.getContent());
        response.put("meta", meta);

        return ResponseEntity.ok(response);
    }
}
>>>>>>> 4d0036690621c93b680f4a1863ad2e8a4f3d0c24
