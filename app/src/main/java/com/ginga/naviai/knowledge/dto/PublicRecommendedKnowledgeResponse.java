package com.ginga.naviai.knowledge.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 公開トップ画面の「今週の注目」レスポンスDTO。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PublicRecommendedKnowledgeResponse {

    private List<PublicRecommendedKnowledgeItemDto> items;
}
