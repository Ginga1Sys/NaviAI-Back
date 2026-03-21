package com.ginga.naviai.knowledge.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

/**
 * 公開トップ画面（SCR-12）向けの記事情報DTO。
 * 認証不要エンドポイント（/api/v1/public/knowledge）で返却する。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PublicKnowledgeItemDto {

    private Long id;
    private String title;
    private String excerpt;
    private String thumbnail;
    private String authorDisplayName;
    private Instant publishedAt;
    private List<String> tags;
}
