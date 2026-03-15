package com.ginga.naviai.knowledge.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 公開トップ画面の「今週の注目」向けおすすめ記事DTO。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PublicRecommendedKnowledgeItemDto {

    private Long id;
    private String title;
    private long likeCount;
}
