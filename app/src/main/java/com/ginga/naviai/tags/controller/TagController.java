package com.ginga.naviai.tags.controller;

import com.ginga.naviai.tags.dto.TagResponse;
import com.ginga.naviai.tags.service.TagService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class TagController {

    private final TagService tagService;

    /** 認証済みユーザー向け: 全公開済み記事のタグ一覧 */
    @GetMapping("/api/v1/tags")
    public ResponseEntity<List<TagResponse>> getAllTags() {
        return ResponseEntity.ok(tagService.getAllTags());
    }

    /** 未認証ユーザー向け: 公開記事（visibility=public）に紐づくタグ一覧 */
    @GetMapping("/api/v1/public/tags")
    public ResponseEntity<List<TagResponse>> getPublicTags() {
        return ResponseEntity.ok(tagService.getPublicTags());
    }
}
