package com.ginga.naviai.knowledge.controller;

import com.ginga.naviai.knowledge.dto.KnowledgeResponse;
import org.springframework.security.core.userdetails.UserDetails;
import com.ginga.naviai.knowledge.service.KnowledgeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/knowledge")
public class KnowledgeController {

    @Autowired
    private KnowledgeService knowledgeService;

    @GetMapping
        public ResponseEntity<?> getKnowledge(
            @RequestParam(required = false) boolean mine,
            @RequestParam(required = false) Long author_id,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int per_page,
            @RequestParam(defaultValue = "createdAt") String sort,
            @AuthenticationPrincipal UserDetails userDetails) {

        Pageable pageable = PageRequest.of(page - 1, per_page, Sort.by(sort).descending());

        Page<KnowledgeResponse> knowledgePage;

        if (mine) {
            if (userDetails == null) {
                return ResponseEntity.status(401).body("Unauthorized");
            }
            knowledgePage = knowledgeService.getMyKnowledgeByUsername(userDetails.getUsername(), pageable);
        } else if (author_id != null) {
            knowledgePage = knowledgeService.getKnowledgeByAuthorId(author_id, pageable);
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
