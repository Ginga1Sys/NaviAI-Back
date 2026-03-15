package com.ginga.naviai.knowledge.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 公開トップ画面向けのページングレスポンスDTO。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PublicKnowledgePageResponse {

    private int page;
    private int size;
    private long totalElements;
    private List<PublicKnowledgeItemDto> items;
}
